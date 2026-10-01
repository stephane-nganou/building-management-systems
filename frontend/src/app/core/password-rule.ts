/**
 * The realm's password policy, checked before a form is sent so the user is not
 * left with a refusal from the server: 12 to 128 characters, and not the email
 * address, which is also the username.
 */
export function acceptablePassword(password: string, email = ''): boolean {
  return (
    password.length >= 12 &&
    password.length <= 128 &&
    password.toLowerCase() !== email.trim().toLowerCase()
  );
}
