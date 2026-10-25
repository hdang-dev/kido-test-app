# Requirements

## Part A: The assignment (pre-interview test brief, Part 3)
Source: the Google Doc brief. This section summarises it, and the brief wins on any conflict.

Build a simple **authentication and user management** application with:
1. Login
2. An account information page
3. Password change
4. Basic role-based access control between **Admin** and **User**

**Free choices:** the language and framework, the database, and the authentication mechanism.
**Out of scope:** a polished UI, production-level completeness, deployment, and handling every edge case.
**Process:**
- Use AI tools during development.
- Build at least one project-specific **Skill** (see `.claude/skills/`).
- Be able to explain, and take responsibility for, all AI-generated code.

## Part B: Clarified requirements
These pin down the details that Part A leaves open, as agreed with the project owner. `docs/design.md` describes how each one is built.

**Sign up**
- The user enters email, password, and confirm password.
- Reject an invalid email format, or an email that is already registered.
- Username and bio are optional. The default username is the part of the email before the `@`. Usernames are display names and **may be duplicated**.
- After a successful sign-up, the user is logged in automatically and lands on the profile page.

**Login**
- Verify the email and password, then open the home (profile) page.
- Keep the user logged in for a while (a "remember me" option).

**Profile page** (the home page after login)
- Show email, username, and bio.
- The user can edit their username and bio.
- The user can change their password by entering the old password, the new password, and a confirmation.

**User management page**
- Only admins can access it.
- Admins can grant admin permission to users.
- A single seeded super admin is the only one who can revoke admin permission. Super admin cannot be granted to anyone (see `design.md` §5).
