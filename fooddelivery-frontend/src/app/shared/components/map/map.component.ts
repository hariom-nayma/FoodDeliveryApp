import { Component, Input, Output, EventEmitter, ElementRef, ViewEncapsulation } from '@angular/core';
import * as L from 'leaflet';
import { CommonModule } from '@angular/common';
import * as polyline from 'polyline';
@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  template: `<div [id]="mapId" class="map-container"></div>`,
  styleUrls: ['./map.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class MapComponent {
  @Input() center = { lat: 28.6139, lng: 77.2090 };
  @Input() showMarker = true; // Use for single marker mode
  @Input() markers: { lat: number, lng: number, title?: string, icon?: string }[] = [];

  @Output() locationSelected = new EventEmitter<{ lat: number; lng: number, accuracy: number | null }>();
  @Output() mapReady = new EventEmitter<void>();

  public mapId = 'map-' + Math.random().toString(36).substring(2, 9);
  private map!: L.Map;
  private marker?: L.Marker;
  private routeLayer?: L.Polyline;
  private markerGroup?: L.FeatureGroup;

  constructor(private host: ElementRef) { }

  public updateMarkers(markers: { lat: number, lng: number, title?: string, icon?: string }[]) {
    if (!this.map) return;
    
    // Clear existing
    if (this.markerGroup) {
      this.markerGroup.clearLayers();
      this.markerGroup.removeFrom(this.map);
    }
    
    this.markerGroup = L.featureGroup();
    
    markers.forEach(m => {
        const options: L.MarkerOptions = {};
        if (m.icon) {
          options.icon = L.icon({
            iconUrl: m.icon,
            iconSize: [40, 40],
            iconAnchor: [20, 40],
            popupAnchor: [0, -40]
          });
        }
        const marker = L.marker([m.lat, m.lng], options);
        if (m.title) marker.bindPopup(m.title);
        marker.addTo(this.markerGroup!);
    });
    
    this.markerGroup.addTo(this.map);
  }

  public drawRoute(encodedPolyline: string) {
    if (!this.map) return;

    try {
      const coords = polyline.decode(encodedPolyline).map(([lat, lng]) => [lat, lng] as L.LatLngTuple);

      if (this.routeLayer) {
        this.map.removeLayer(this.routeLayer);
      }

      this.routeLayer = L.polyline(coords, { color: 'blue', weight: 4 }).addTo(this.map);
      this.map.fitBounds(this.routeLayer.getBounds(), { padding: [50, 50] });
    } catch (e) {
      console.error('Failed to draw route', e);
    }
  }

  /** Called manually AFTER dialog is visible */
  public initializeMap() {
    // ... existing initialization code ...
    if (this.map) this.map.remove(); // prevent duplicates

    const element = document.getElementById(this.mapId); // Use document.getElementById for safety
    // const element = this.host.nativeElement.querySelector('#' + this.mapId); 
    if (!element) return;

    // Wait slightly to ensure layout
    if (element.clientHeight === 0) {
      setTimeout(() => this.initializeMap(), 100);
      return;
    }

    this.map = L.map(element).setView([this.center.lat, this.center.lng], 14);
    this.mapReady.emit();

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(this.map);

    this.map.on('click', (e: any) => {
      const coords = { lat: e.latlng.lat, lng: e.latlng.lng, accuracy: null };
      if (this.marker) this.marker.remove();
      this.marker = L.marker([coords.lat, coords.lng]).addTo(this.map);
      this.locationSelected.emit(coords);
    });

    if (this.showMarker && this.markers.length === 0) {
      this.marker = L.marker([this.center.lat, this.center.lng]).addTo(this.map);
    }

    // Add custom markers
    if (this.markers.length > 0) {
      const group = L.featureGroup();
      this.markers.forEach(m => {
        const options: L.MarkerOptions = {};
        if (m.icon) {
          options.icon = L.icon({
            iconUrl: m.icon,
            iconSize: [40, 40],
            iconAnchor: [20, 40],
            popupAnchor: [0, -40]
          });
        }
        const marker = L.marker([m.lat, m.lng], options);
        if (m.title) marker.bindPopup(m.title);
        marker.addTo(this.map);
        group.addLayer(marker);
      });
      group.addTo(this.map);
      try {
        this.map.fitBounds(group.getBounds().pad(0.2));
      } catch (e) { }
    }

    // Force resize to fill container
    setTimeout(() => {
      this.map.invalidateSize();
    }, 200);
  }

  public invalidateSize() {
    this.map?.invalidateSize(true);
  }

  /** Detect user location and move map to that point */
  public detectMyLocation() {
    if (!navigator.geolocation) {
      alert("Device does not support GPS");
      return;
    }

    let best: GeolocationPosition | null = null;
    let watchId = 0;

    const finish = () => {
      navigator.geolocation.clearWatch(watchId);
      if (!best) {
        alert("Unable to get accurate location");
        return;
      }
      const { latitude, longitude, accuracy } = best.coords;
      console.log("FINAL BEST ACCURACY:", accuracy);

      this.map.setView([latitude, longitude], 17);

      if (this.marker) this.marker.remove();
      this.marker = L.marker([latitude, longitude]).addTo(this.map);

      this.locationSelected.emit({ lat: latitude, lng: longitude, accuracy });
    }

    // Step 1: Force Refresh GPS Lock (One shot)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        if (!best || pos.coords.accuracy < best.coords.accuracy) best = pos;
      },
      () => { },
      { enableHighAccuracy: true, maximumAge: 0, timeout: 5000 } // Extended timeout
    );

    // Step 2: Now use watchPosition to get BEST accuracy over short time
    watchId = navigator.geolocation.watchPosition(
      (pos) => {
        if (!best || pos.coords.accuracy < best.coords.accuracy) {
          best = pos;
        }
      },
      (err) => console.error(err),
      { enableHighAccuracy: true, maximumAge: 0, timeout: 15000 }
    );

    // Step 3: Stop after 3s
    setTimeout(finish, 3000);
  }
}
