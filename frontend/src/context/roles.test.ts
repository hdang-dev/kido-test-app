import type { Role } from '../api/types';
import { roleActionFor } from './roles';

const actor = (role: Role) => ({ id: 'me', role });
const target = (role: Role, id = 'them') => ({ id, role });

// Mirrors design.md §5 and AdminUserService.changeRole
describe('roleActionFor', () => {
  it.each<Role>(['ADMIN', 'SUPER_ADMIN'])('%s can grant admin to a USER', (role) => {
    expect(roleActionFor(actor(role), target('USER'))).toEqual({ kind: 'grant' });
  });

  it('only the SUPER_ADMIN can revoke an ADMIN', () => {
    expect(roleActionFor(actor('SUPER_ADMIN'), target('ADMIN'))).toEqual({ kind: 'revoke' });
    expect(roleActionFor(actor('ADMIN'), target('ADMIN'))).toMatchObject({ kind: 'none' });
  });

  it('nobody acts on themselves or on the SUPER_ADMIN', () => {
    expect(roleActionFor(actor('SUPER_ADMIN'), target('SUPER_ADMIN', 'me'))).toEqual({ kind: 'none', reason: 'You' });
    expect(roleActionFor(actor('ADMIN'), target('SUPER_ADMIN'))).toEqual({ kind: 'none', reason: 'Protected' });
  });
});
