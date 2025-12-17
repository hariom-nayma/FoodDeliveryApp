import { Injectable } from '@angular/core';
import { Socket } from 'ngx-socket-io';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class SocketService {

    constructor(private socket: Socket) { }

    joinRiderRoom(userId: string) {
        this.socket.emit('join_room', `rider_${userId}`);
    }

    joinUserRoom(userId: string) {
        this.socket.emit('join_room', `user_${userId}`);
    }

    onAssignmentRequest(): Observable<any> {
        return this.socket.fromEvent('assignment_request');
    }

    onOrderUpdate(): Observable<any> {
        return this.socket.fromEvent('order_update');
    }

    onOrderEscalated(): Observable<any> {
        return this.socket.fromEvent('order_escalated');
    }

    // Generic emit/listen
    emit(event: string, payload: any) {
        this.socket.emit(event, payload);
    }

    listen(event: string): Observable<any> {
        return this.socket.fromEvent(event);
    }

    emitLocation(userId: string, lat: number, lng: number) {
        this.socket.emit('update_location', { userId, lat, lng });
    }
}
