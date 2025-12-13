import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class NavigationService {

    private apiUrl = environment.apiUrl + '/navigation';

    constructor(private http: HttpClient) { }

    getRoute(fromLat: number, fromLng: number, toLat: number, toLng: number): Observable<any> {
        console.log('NavigationService: requesting route', fromLat, fromLng, toLat, toLng);
        return this.http.get<any>(`${this.apiUrl}/route`, {
            params: {
                fromLat: fromLat.toString(),
                fromLng: fromLng.toString(),
                toLat: toLat.toString(),
                toLng: toLng.toString()
            }
        });
    }
}
