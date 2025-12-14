import { Component, inject, signal, Input, OnInit } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { RestaurantService } from '../../core/services/restaurant.service';
import { DocumentUploadRequest } from '../../core/restaurant/restaurant.types';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './document-upload.component.html',
  styles: [`
    .container { max-width: 600px; margin: 2rem auto; padding: 1rem; }
    .card { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
    h2 { margin-bottom: 1rem; color: #333; }
    p { margin-bottom: 2rem; color: #666; }
    .input-group { margin-bottom: 1rem; }
    label { display: block; margin-bottom: 0.5rem; font-weight: 500; }
    input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; }
    button { background: var(--primary-color); color: white; border: none; padding: 1rem; border-radius: 4px; width: 100%; cursor: pointer; }
    button:disabled { background: #ccc; }
    .error { color: red; margin-bottom: 1rem; }
  `]
})
export class DocumentUploadComponent implements OnInit {
  private fb = inject(NonNullableFormBuilder);
  private restaurantService = inject(RestaurantService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  restaurantId = '';

  form = this.fb.group({
    gstNumber: ['', Validators.required],
    fssaiNumber: ['', Validators.required],
    gstCertificateUrl: ['', Validators.required], // URL for now
    fssaiLicenseUrl: ['', Validators.required], // URL for now
    panCardUrl: ['', Validators.required] // URL for now
  });

  loading = signal(false);
  error = signal('');

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.restaurantId = params.get('id') || '';
    });
  }

  onSubmit() {
    if (this.form.invalid || !this.restaurantId) return;

    this.loading.set(true);
    this.error.set('');

    const val = this.form.getRawValue();

    const request: DocumentUploadRequest = {
      gstNumber: val.gstNumber,
      fssaiNumber: val.fssaiNumber,
      documents: [
        { type: 'GST_CERTIFICATE', fileUrl: val.gstCertificateUrl },
        { type: 'FSSAI_LICENSE', fileUrl: val.fssaiLicenseUrl },
        { type: 'PAN_CARD', fileUrl: val.panCardUrl }
      ]
    };

    this.restaurantService.uploadDocuments(this.restaurantId, request).subscribe({
      next: () => {
        // Backend auto-submits if docs are valid
        this.router.navigate(['/restaurant', 'pending']);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Upload failed');
        this.loading.set(false);
      }
    });
  }
}
