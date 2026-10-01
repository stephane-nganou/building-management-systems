import { acceptablePassword } from './password-rule';

describe('acceptablePassword', () => {
  it('asks for twelve characters', () => {
    expect(acceptablePassword('elevenchars')).toBe(false);
    expect(acceptablePassword('twelve-chars')).toBe(true);
  });

  it('refuses more than 128 characters', () => {
    expect(acceptablePassword('x'.repeat(128))).toBe(true);
    expect(acceptablePassword('x'.repeat(129))).toBe(false);
  });

  it('refuses the email address, whatever its case', () => {
    expect(acceptablePassword('Nina@Example.com', 'nina@example.com')).toBe(false);
    expect(acceptablePassword('a-good-secret', 'nina@example.com')).toBe(true);
  });
});
