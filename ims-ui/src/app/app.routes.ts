import { Routes } from '@angular/router';
import { AdminLayout } from './layout/admin-layout';
import { DashboardPage } from './features/dashboard/dashboard.page';
import { LoginPage } from './features/auth/login.page';
import { DesignSystemPage } from './features/design-system/design-system.page';
import { StudentsListPage } from './features/students/students-list.page';
import { StudentCreatePage } from './features/students/student-create.page';
import { StudentDetailPage } from './features/students/student-detail.page';
import { StudentEditPage } from './features/students/student-edit.page';
import { FacultyListPage } from './features/faculty/faculty-list.page';
import { FacultyCreatePage } from './features/faculty/faculty-create.page';
import { FacultyDetailPage } from './features/faculty/faculty-detail.page';
import { FacultyEditPage } from './features/faculty/faculty-edit.page';
import { StaffListPage } from './features/staff/staff-list.page';
import { StaffCreatePage } from './features/staff/staff-create.page';
import { StaffDetailPage } from './features/staff/staff-detail.page';
import { StaffEditPage } from './features/staff/staff-edit.page';
import { CoursesListPage } from './features/academic/courses-list.page';
import { CourseCreatePage } from './features/academic/course-create.page';
import { CourseDetailPage } from './features/academic/course-detail.page';
import { CourseEditPage } from './features/academic/course-edit.page';
import { FeePlanEditPage } from './features/academic/fee-plan-edit.page';
import { BatchesListPage } from './features/academic/batches-list.page';
import { BatchCreatePage } from './features/academic/batch-create.page';
import { BatchDetailPage } from './features/academic/batch-detail.page';
import { BatchEditPage } from './features/academic/batch-edit.page';
import { AdmissionsListPage } from './features/admissions/admissions-list.page';
import { AdmissionCreatePage } from './features/admissions/admission-create.page';
import { AdmissionDetailPage } from './features/admissions/admission-detail.page';
import { EnquiriesListPage } from './features/admissions/enquiries-list.page';
import { EnquiryCreatePage } from './features/admissions/enquiry-create.page';
import { EnquiryDetailPage } from './features/admissions/enquiry-detail.page';
import { EnrollmentsListPage } from './features/enrollments/enrollments-list.page';
import { EnrollmentCreatePage } from './features/enrollments/enrollment-create.page';
import { EnrollmentDetailPage } from './features/enrollments/enrollment-detail.page';
import { OutstandingListPage } from './features/finance/outstanding-list.page';
import { InstitutesListPage } from './features/institutes/institutes-list.page';
import { InstituteCreatePage } from './features/institutes/institute-create.page';
import { InstituteDetailPage } from './features/institutes/institute-detail.page';
import { InstituteEditPage } from './features/institutes/institute-edit.page';
import { UsersListPage } from './features/users/users-list.page';
import { UserCreatePage } from './features/users/user-create.page';
import { UserDetailPage } from './features/users/user-detail.page';
import { UserEditPage } from './features/users/user-edit.page';
import {
  authGuard,
  guestGuard,
  instituteOperatorGuard,
  platformAdminGuard,
} from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPage,
    canActivate: [guestGuard],
  },
  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'users',
        component: UsersListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'users/new',
        component: UserCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'users/:id/edit',
        component: UserEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'users/:id',
        component: UserDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'dashboard',
        component: DashboardPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'design-system',
        component: DesignSystemPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'students',
        component: StudentsListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'students/new',
        component: StudentCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'students/:id/edit',
        component: StudentEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'students/:id',
        component: StudentDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'faculty',
        component: FacultyListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'faculty/new',
        component: FacultyCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'faculty/:id/edit',
        component: FacultyEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'faculty/:id',
        component: FacultyDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'staff',
        component: StaffListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'staff/new',
        component: StaffCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'staff/:id/edit',
        component: StaffEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'staff/:id',
        component: StaffDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'courses',
        component: CoursesListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'courses/new',
        component: CourseCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'courses/:id/edit',
        component: CourseEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'courses/:id',
        component: CourseDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'fee-plans/:id/edit',
        component: FeePlanEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'batches',
        component: BatchesListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'batches/new',
        component: BatchCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'batches/:id/edit',
        component: BatchEditPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'batches/:id',
        component: BatchDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'admissions',
        component: AdmissionsListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'admissions/new',
        component: AdmissionCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'admissions/:id',
        component: AdmissionDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'enquiries',
        component: EnquiriesListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'enquiries/new',
        component: EnquiryCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'enquiries/:id',
        component: EnquiryDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'enrollments',
        component: EnrollmentsListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'enrollments/new',
        component: EnrollmentCreatePage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'enrollments/:id',
        component: EnrollmentDetailPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'outstanding',
        component: OutstandingListPage,
        canActivate: [instituteOperatorGuard],
      },
      {
        path: 'institutes',
        component: InstitutesListPage,
        canActivate: [platformAdminGuard],
      },
      {
        path: 'institutes/new',
        component: InstituteCreatePage,
        canActivate: [platformAdminGuard],
      },
      {
        path: 'institutes/:id',
        component: InstituteDetailPage,
        canActivate: [platformAdminGuard],
      },
      {
        path: 'institutes/:id/edit',
        component: InstituteEditPage,
        canActivate: [platformAdminGuard],
      },
    ],
  },
];
