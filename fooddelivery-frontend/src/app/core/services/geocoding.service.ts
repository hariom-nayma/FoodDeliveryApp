import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class GeocodingService {
    constructor(private http: HttpClient) { }

    reverseGeocode(lat: number, lon: number): Observable<any> {
        return this.http.get(`${environment.apiUrl}/location/reverse?lat=${lat}&lon=${lon}`).pipe(
            map((res: any) => {
                if (res.features && res.features.length > 0) {
                    const feature = res.features[0];
                    const context = feature.context || [];
                    const city = context.find((c: any) => c.id.startsWith('municipal_district') || c.id.startsWith('county') || c.id.startsWith('city'))?.text || '';
                    const state = context.find((c: any) => c.id.startsWith('region') || c.id.startsWith('state'))?.text || '';
                    const postcode = context.find((c: any) => c.id.startsWith('postal_code'))?.text || '';

                    return {
                        line1: feature.place_name || '',
                        city: city, // Fallback might be needed if Nominatim structure differs slightly in text content
                        state: state,
                        postalCode: postcode,
                        displayName: feature.place_name || ''
                    };
                }
                return {
                    line1: '',
                    city: '',
                    state: '',
                    postalCode: '',
                    displayName: ''
                };
            })
        );
    }
}
