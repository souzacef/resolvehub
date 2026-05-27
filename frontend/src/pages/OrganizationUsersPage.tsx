import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../features/auth/AuthContext';
import {
  INVALID_EMAIL_MESSAGE,
  isEmailFormatValid,
} from '../features/auth/emailValidation';
import {
  PASSWORD_POLICY_HINT,
  isPasswordPolicyCompliant,
  validatePasswordPolicy,
} from '../features/auth/passwordPolicy';
import {
  createOrganizationUser,
  listOrganizationUsers,
} from '../features/tickets/tickets';
import { isApiError } from '../lib/apiClient';
import type { OrganizationUserResponse, UserRole } from '../types/api';

const adminAllowedRoles: UserRole[] = ['CUSTOMER', 'AGENT', 'MANAGER', 'ADMIN'];
const managerAllowedRoles: UserRole[] = ['CUSTOMER', 'AGENT'];

export function OrganizationUsersPage() {
  const { role } = useAuth();

  const canManageUsers = role === 'ADMIN' || role === 'MANAGER';

  const allowedRoles = useMemo<UserRole[]>(() => {
    if (role === 'ADMIN') {
      return adminAllowedRoles;
    }

    if (role === 'MANAGER') {
      return managerAllowedRoles;
    }

    return [];
  }, [role]);

  const [users, setUsers] = useState<OrganizationUserResponse[]>([]);
  const [isLoadingUsers, setIsLoadingUsers] = useState(true);
  const [usersError, setUsersError] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [emailTouched, setEmailTouched] = useState(false);
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [newRole, setNewRole] = useState<UserRole>('CUSTOMER');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);

  const emailIsInvalid = emailTouched && !isEmailFormatValid(email);
  const emailIsValid = emailTouched && email.trim().length > 0 && !emailIsInvalid;
  const passwordHasValue = password.length > 0;
  const passwordIsValid = passwordHasValue && isPasswordPolicyCompliant(password);
  const passwordPolicyError =
    passwordHasValue && !passwordIsValid ? validatePasswordPolicy(password) : null;

  useEffect(() => {
    if (!canManageUsers) {
      setIsLoadingUsers(false);
      setUsers([]);
      setUsersError(null);
      return;
    }

    let isMounted = true;

    async function loadUsers() {
      setIsLoadingUsers(true);
      setUsersError(null);

      try {
        const data = await listOrganizationUsers();
        if (!isMounted) {
          return;
        }

        setUsers(data);
      } catch (error) {
        if (!isMounted) {
          return;
        }

        setUsers([]);
        setUsersError(mapUsersLoadError(error));
      } finally {
        if (isMounted) {
          setIsLoadingUsers(false);
        }
      }
    }

    void loadUsers();

    return () => {
      isMounted = false;
    };
  }, [canManageUsers]);

  useEffect(() => {
    if (!allowedRoles.includes(newRole) && allowedRoles.length > 0) {
      setNewRole(allowedRoles[0]);
    }
  }, [allowedRoles, newRole]);

  async function handleCreateUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!canManageUsers) {
      setSubmitError('You do not have permission to create this user.');
      return;
    }

    setEmailTouched(true);
    setSubmitError(null);
    setSubmitSuccess(null);

    const hasRequiredFields =
      name.trim().length > 0 && email.trim().length > 0 && password.length > 0;

    if (!hasRequiredFields) {
      setSubmitError('Please check the user fields and try again.');
      return;
    }

    if (!isEmailFormatValid(email)) {
      setSubmitError(INVALID_EMAIL_MESSAGE);
      return;
    }

    const passwordValidationError = validatePasswordPolicy(password);
    if (passwordValidationError !== null) {
      return;
    }

    setIsSubmitting(true);

    try {
      await createOrganizationUser({
        name: name.trim(),
        email: email.trim(),
        password,
        role: newRole,
      });

      setSubmitSuccess('User created successfully.');
      setName('');
      setEmail('');
      setEmailTouched(false);
      setPassword('');
      setShowPassword(false);
      if (allowedRoles.length > 0) {
        setNewRole(allowedRoles[0]);
      }

      const refreshedUsers = await listOrganizationUsers();
      setUsers(refreshedUsers);
    } catch (error) {
      setSubmitError(mapCreateUserError(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  function mapUsersLoadError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Service is unavailable. Please try again later.';
    }

    if (error.kind === 'network') {
      return 'Service is unavailable. Please try again later.';
    }

    if (error.status === 403) {
      return 'You do not have permission to view organization users.';
    }

    return 'Service is unavailable. Please try again later.';
  }

  function mapCreateUserError(error: unknown): string {
    if (!isApiError(error)) {
      return 'Service is unavailable. Please try again later.';
    }

    if (error.kind === 'network') {
      return 'Service is unavailable. Please try again later.';
    }

    if (error.status === 409) {
      return 'This email is already registered.';
    }

    if (error.status === 403) {
      return 'You do not have permission to create this user.';
    }

    if (
      error.responseBody !== null &&
      error.responseBody.includes(PASSWORD_POLICY_HINT)
    ) {
      return PASSWORD_POLICY_HINT;
    }

    if (error.status === 400 || error.status === 422) {
      return 'Please check the user fields and try again.';
    }

    return 'Service is unavailable. Please try again later.';
  }

  function formatUserDisplayName(user: OrganizationUserResponse): string {
    if (user.name && user.name.trim().length > 0) {
      return user.name;
    }

    return 'Unnamed user';
  }

  if (!canManageUsers) {
    return (
      <section>
        <header className="page-header">
          <h1>Organization Users</h1>
        </header>
        <p className="state-panel state-error" role="alert">
          You do not have permission to view this page.
        </p>
      </section>
    );
  }

  return (
    <section className="page-section-stack">
      <header className="page-header">
        <h1>Organization Users</h1>
      </header>

      <section className="form-card" aria-label="Create organization user">
        <h2>Create User</h2>

        <form onSubmit={handleCreateUser} noValidate>
          <div className="form-grid">
            <div>
              <label htmlFor="org-user-name">Name</label>
              <input
                id="org-user-name"
                type="text"
                autoComplete="name"
                required
                maxLength={120}
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </div>

            <div>
              <label htmlFor="org-user-email">Email</label>
              <input
                id="org-user-email"
                type="text"
                autoComplete="email"
                required
                maxLength={255}
                value={email}
                className={emailIsInvalid ? 'input-invalid' : emailIsValid ? 'input-valid' : ''}
                onChange={(event) => {
                  const nextValue = event.target.value;
                  setEmail(nextValue);
                  if (!emailTouched && nextValue.length > 0) {
                    setEmailTouched(true);
                  }
                }}
                onBlur={() => setEmailTouched(true)}
              />
              <p
                className={`field-feedback ${
                  emailIsInvalid ? 'field-feedback-error' : 'field-feedback-muted'
                }`}
                role={emailIsInvalid ? 'alert' : undefined}
              >
                {emailIsInvalid ? INVALID_EMAIL_MESSAGE : ' '}
              </p>
            </div>

            <div>
              <label htmlFor="org-user-password">Password</label>
              <div className="password-input-wrapper">
                <input
                  id="org-user-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  required
                  maxLength={72}
                  className={
                    passwordHasValue
                      ? passwordIsValid
                        ? 'input-valid'
                        : 'input-invalid'
                      : ''
                  }
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
                <button
                  type="button"
                  className="password-toggle-button"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword((currentValue) => !currentValue)}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
              <p
                className={`field-feedback ${
                  passwordHasValue
                    ? passwordIsValid
                      ? 'field-feedback-success'
                      : 'field-feedback-error'
                    : 'field-feedback-muted'
                }`}
                role={passwordHasValue && !passwordIsValid ? 'alert' : undefined}
              >
                {passwordHasValue
                  ? passwordIsValid
                    ? 'Password meets requirements.'
                    : passwordPolicyError
                  : PASSWORD_POLICY_HINT}
              </p>
            </div>

            <div>
              <label htmlFor="org-user-role">Role</label>
              <select
                id="org-user-role"
                value={newRole}
                onChange={(event) => setNewRole(event.target.value as UserRole)}
              >
                {allowedRoles.map((candidateRole) => (
                  <option key={candidateRole} value={candidateRole}>
                    {candidateRole}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {submitError ? (
            <p className="state-panel state-error" role="alert">
              {submitError}
            </p>
          ) : null}

          {submitSuccess ? (
            <p className="state-panel" role="status">
              {submitSuccess}
            </p>
          ) : null}

          <div className="form-actions">
            <button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Creating user...' : 'Create user'}
            </button>
          </div>
        </form>
      </section>

      {isLoadingUsers ? <p className="state-panel">Loading organization users...</p> : null}

      {!isLoadingUsers && usersError ? (
        <p className="state-panel state-error" role="alert">
          {usersError}
        </p>
      ) : null}

      {!isLoadingUsers && !usersError && users.length === 0 ? (
        <p className="state-panel muted-text">No users found in this organization.</p>
      ) : null}

      {!isLoadingUsers && !usersError && users.length > 0 ? (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{formatUserDisplayName(user)}</td>
                  <td>{user.email}</td>
                  <td>{user.role}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
