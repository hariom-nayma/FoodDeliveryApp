import { Component, Inject, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

import { MapComponent } from '../map/map.component';
import { GeocodingService } from '../../../core/services/geocoding.service';
import { UserService } from '../../../core/services/user.service';

@Component({
    selector: 'app-add-address',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, MapComponent, MatDialogModule, MatButtonModule],
    templateUrl: './add-address.component.html',
    styleUrls: ['./add-address.component.css']
})
export class AddAddressComponent implements OnInit, AfterViewInit {
    addressForm!: FormGroup;
    selectedLocation: any;
    accuracy: number | null = null;

    @ViewChild('mapRef') mapComponent!: MapComponent;

    isEditMode = false;
    addressId: string | null = null;

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private geocode: GeocodingService,
        public dialogRef: MatDialogRef<AddAddressComponent>,
        @Inject(MAT_DIALOG_DATA) public data: any
    ) { }

    ngOnInit(): void {
        this.addressForm = this.fb.group({
            line1: ['', Validators.required],
            line2: [''],
            city: ['', Validators.required],
            state: ['', Validators.required],
            postalCode: ['', Validators.required],
            latitude: [0, Validators.required],
            longitude: [0, Validators.required],
            label: ['Home']
        });

        if (this.data && this.data.address) {
            this.isEditMode = true;
            this.addressId = this.data.address.addressId;
            this.addressForm.patchValue(this.data.address);
        }
    }

    ngAfterViewInit() {
        // Initialize map after view is ready
        if (this.mapComponent) {
            // slight delay to allow dialog open animation to finish/layout
            setTimeout(() => {
                this.mapComponent.initializeMap();
                // If editing, set marker
                if (this.isEditMode) {
                    // Need to add method to map to set manual marker?
                    // MapComponent inputs center but doesn't auto-set marker unless clicked?
                    // Re-implementing MapComponent.center handling logic might be needed or logic in MapComponent
                    // For now, center map on address
                    // Wait, existing MapComponent implementation uses @Input center.
                }
            }, 300);
        }
    }

    detectLocation() {
        this.mapComponent.detectMyLocation();
    }

    onLocationPicked(loc: any) {
        this.selectedLocation = loc;
        this.accuracy = loc.accuracy;

        // 1️⃣ Set coordinates immediately
        this.addressForm.patchValue({
            latitude: loc.lat,
            longitude: loc.lng
        });

        // 2️⃣ Reverse Geocode (Address Auto-fill)
        this.geocode.reverseGeocode(loc.lat, loc.lng).subscribe(addr => {
            // Only patch if empty or user wants? usually overwrite is fine for "detect"
            this.addressForm.patchValue({
                line1: addr.line1 || '',
                city: addr.city || '',
                state: addr.state || '',
                postalCode: addr.postalCode || ''
            });
        });
    }

    get accuracyColor(): string {
        if (this.accuracy === null) {
            return '';
        }
        if (this.accuracy < 20) {
            return 'text-green-600';
        } else if (this.accuracy < 50) {
            return 'text-orange-600';
        } else {
            return 'text-red-600';
        }
    }

    onAddAddress() {
        if (this.addressForm.valid) {
            if (this.isEditMode && this.addressId) {
                this.userService.updateAddress(this.addressId, this.addressForm.value).subscribe({
                    next: () => {
                        this.dialogRef.close(true);
                    },
                    error: (err) => console.error(err)
                });
            } else {
                this.userService.addAddress(this.addressForm.value).subscribe({
                    next: () => {
                        this.dialogRef.close(true);
                    },
                    error: (err) => console.error(err)
                });
            }
        }
    }

    onCancel(): void {
        this.dialogRef.close();
    }
}
