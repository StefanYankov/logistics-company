import {Component, signal} from '@angular/core';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-reports-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports-dashboard.html',
  styleUrl: './reports-dashboard.css'
})
export class ReportsDashboard {
  activeTab = signal<'revenue' | 'shipments'>('revenue');

  selectTab(tab: 'revenue' | 'shipments') {
    this.activeTab.set(tab);
  }
}
