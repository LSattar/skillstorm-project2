import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly apiUrl = `${environment.apiBaseUrl}/reports`;

  constructor(private http: HttpClient) {}

  generatePaymentTransactionReport(from: string, to: string) {
    // Returns a blob (CSV or XLSX)
    return this.http.get(`${this.apiUrl}/payment-transactions`, {
      params: { from, to },
      responseType: 'blob',
      withCredentials: true,
    });
  }
}
