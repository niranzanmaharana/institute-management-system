import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-reports-page',
  imports: [RouterLink, UiCard],
  templateUrl: './reports.page.html',
  styleUrl: './reports-shared.scss',
})
export class ReportsPage {
  private readonly auth = inject(AuthService);

  get canStudents(): boolean {
    return this.auth.hasPermission('student:read');
  }

  get canAdmissions(): boolean {
    return (
      this.auth.hasPermission('admission:write') ||
      this.auth.hasPermission('admission:approve') ||
      this.auth.hasPermission('student:read')
    );
  }

  get canAttendance(): boolean {
    return (
      this.auth.hasPermission('attendance:mark') || this.auth.hasPermission('attendance:unlock')
    );
  }

  get canExams(): boolean {
    return this.auth.hasPermission('exam:mark') || this.auth.hasPermission('exam:publish');
  }

  get canFinance(): boolean {
    return this.auth.hasPermission('fee:read');
  }
}
