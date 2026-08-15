import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('persists tokens after login', () => {
    service.login('admin', 'Password@123', 'DEMO_A').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/v1/auth/login`);
    expect(req.request.body).toEqual({
      username: 'admin',
      password: 'Password@123',
      instituteCode: 'DEMO_A',
    });
    req.flush({
      accessToken: 'access',
      refreshToken: 'refresh',
      user: {
        id: 1,
        username: 'admin',
        email: 'a@example.com',
        instituteId: 1,
        roles: ['ADMIN'],
        permissions: [],
      },
    });
    expect(service.isAuthenticated()).toBe(true);
    expect(service.user()?.username).toBe('admin');
  });
});
