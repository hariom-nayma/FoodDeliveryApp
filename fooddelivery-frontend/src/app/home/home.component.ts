import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Restaurant, RestaurantService } from '../core/services/restaurant.service';
import { GeocodingService } from '../core/services/geocoding.service';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [RouterLink],
    templateUrl: './home.component.html',
    styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
    private restaurantService = inject(RestaurantService);
    private geocodingService = inject(GeocodingService);

    restaurants = signal<Restaurant[]>([]);
    loading = signal(true);
    locationStatus = signal<string>('Detecting location...');
    detectedCity = signal<string | null>(null);

    ngOnInit() {
        this.detectLocation();
    }

    detectLocation() {
        this.loading.set(true);
        this.locationStatus.set('Detecting location...');

        if (!navigator.geolocation) {
            this.locationStatus.set('Geolocation not supported');
            this.loadRestaurants(null);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (pos) => {
                this.locationStatus.set('Found location, identifying city...');
                this.geocodingService.reverseGeocode(pos.coords.latitude, pos.coords.longitude).subscribe({
                    next: (addr) => {
                        const city = addr.city || addr.state || ''; // Fallback
                        this.detectedCity.set(city);
                        this.locationStatus.set(`Location: ${city}`);
                        this.loadRestaurants(city);
                    },
                    error: () => {
                        this.locationStatus.set('Could not identify city');
                        this.loadRestaurants(null);
                    }
                });
            },
            (err) => {
                console.error(err);
                this.locationStatus.set('Location access denied');
                this.loadRestaurants(null);
            },
            { timeout: 10000, enableHighAccuracy: true }
        );
    }

    loadRestaurants(city: string | null) {
        this.restaurantService.getRestaurants(city || undefined).subscribe({
            next: (data) => {
                this.restaurants.set(data);
                this.loading.set(false);
            },
            error: (err) => {
                console.error(err);
                this.loading.set(false);
            }
        });
    }
}
