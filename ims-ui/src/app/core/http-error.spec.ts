import { HttpErrorResponse } from '@angular/common/http';
import { httpErrorMessage, httpLoadError, isNetworkFailure } from './http-error';

describe('httpErrorMessage', () => {
  it('uses API message for 401', () => {
    const err = new HttpErrorResponse({
      status: 401,
      error: { status: 401, error: 'UNAUTHORIZED', message: 'Authentication required' },
    });
    expect(httpErrorMessage(err, 'Could not load students.')).toBe('Authentication required');
  });

  it('falls back to auth copy when 401 has no body message', () => {
    const err = new HttpErrorResponse({ status: 401, error: null });
    expect(httpErrorMessage(err, 'Could not load students.')).toContain('Authentication required');
  });

  it('reports network failure for status 0', () => {
    const err = new HttpErrorResponse({ status: 0, statusText: 'Unknown Error', error: new ProgressEvent('error') });
    expect(httpErrorMessage(err, 'Could not load students.')).toContain('Cannot reach the server');
    expect(isNetworkFailure(err)).toBe(true);
  });

  it('uses friendly copy for 5xx without API message', () => {
    const err = new HttpErrorResponse({ status: 500, error: { status: 500, error: 'INTERNAL' } });
    expect(httpErrorMessage(err, 'Could not load students.')).toContain('server encountered an error');
  });

  it('uses fallback for ordinary client errors', () => {
    const err = new HttpErrorResponse({ status: 400, error: null });
    expect(httpErrorMessage(err, 'Could not create student.')).toBe('Could not create student.');
  });
});

describe('httpLoadError', () => {
  it('maps 404 to not-found label', () => {
    const err = new HttpErrorResponse({ status: 404, error: null });
    expect(httpLoadError(err, 'student')).toBe('Student not found.');
  });

  it('prefers 401 message over load fallback', () => {
    const err = new HttpErrorResponse({
      status: 401,
      error: { message: 'Authentication required' },
    });
    expect(httpLoadError(err, 'student')).toBe('Authentication required');
  });
});
