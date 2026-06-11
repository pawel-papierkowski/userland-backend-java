-- ============================================================================
-- USERS
-- All users below (except last one) are just to stuff tables, so admin panel has something to show off. They are DEMO.

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-01 10:43:49', '2026-04-01 10:43:59', 'Łukasz Luga', 'little.lamb@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Łukasz', 'Luga');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 10:43:49', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 10:43:59', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 10:47:19', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'LOGIN', '');
INSERT INTO iam.jwt(id_user, created_at, expires_at, token)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', (CURRENT_TIMESTAMP + INTERVAL '1 day') AT TIME ZONE 'UTC', 'eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3Nzk4OTA1MDAsImV4cCI6MTc3OTkxMjEwMH0.vRpmS6yPCnEYK6n906XvIYyFBZxCWoWaUqzWnSppvrs');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-01 11:18:20', '2026-04-01 11:18:29', 'Whatever', 'insufficient@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Ęcie', 'Pęcie');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 11:18:20', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 11:18:29', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 11:18:40', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-01 13:42:01', '2026-04-01 13:42:13', 'ErghMerGerdh', 'nbwuieh22@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), '', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 13:42:01', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 13:42:13', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 13:48:31', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-01 15:27:36', '2026-04-01 15:27:46', 'Blabla', 'jsejdbs@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Blabla', null);
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 15:27:36', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 15:27:46', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 15:28:01', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-01 18:03:38', '2026-04-01 19:24:57', 'Ena', 'bartek.bart@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Bartek', 'Bartosz');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 18:03:38', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 19:24:57', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-01 19:26:22', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-07 19:23:56', '2026-04-07 19:24:36', 'Mewka', 'mewkamewka@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Aneta', 'Mewka');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-07 19:23:56', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-07 19:24:36', 'USER', 'ACTIVATE', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-09 17:31:48', '2026-04-09 17:31:54', 'Szypuga', 'szypuga@test.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Antonin', 'Mewka');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-09 17:31:48', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-09 17:31:54', 'USER', 'ACTIVATE', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-10 12:12:04', '2026-04-10 12:12:15', 'Jan44', 'jan.kowalski@test.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Jan', 'Kowalski');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-10 12:12:04', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-10 12:12:15', 'USER', 'ACTIVATE', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-10 21:46:48', '2026-04-10 21:46:59', 'Nadkonduktor', 'nadkonduktor@example.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'en', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), null, null);
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-10 21:46:48', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-10 21:46:59', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-10 21:47:31', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-10 21:48:14', 'USER', 'EDIT', 'username');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-11 12:01:03', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-12 18:27:22', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 23:13:28', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-24 16:33:11', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-24 21:21:21', 'USER', 'PROLONG', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-30 06:09:26', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-20 17:23:41', 'USER', 'PASS_RESET_REQ', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-20 17:24:13', 'USER', 'PASS_RESET', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-20 17:26:36', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-21 18:39:14', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-22 17:55:52', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-13 19:45:00', '2026-04-13 19:45:08', 'IamInnocent', 'something@examplest.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'en', 'DEMO', true);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Robert', 'Nowak');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-13 19:45:00', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-13 19:45:08', 'USER', 'ACTIVATE', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-19 09:32:14', '2026-04-19 10:01:37', 'VeryImportantPerson', 'vip@test.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'en', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Very', 'Important');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 09:32:14', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 10:01:37', 'USER', 'ACTIVATE', '');
INSERT INTO iam.user_permissions(id_user, id_permission, created_at, value)
VALUES ((SELECT max(id) FROM iam.users), 1, '2026-04-19 10:12:35', 'operator');
INSERT INTO iam.user_permissions(id_user, id_permission, created_at, value)
VALUES ((SELECT max(id) FROM iam.users), 2, '2026-04-19 10:12:35', 'view');
INSERT INTO iam.user_permissions(id_user, id_permission, created_at, value)
VALUES ((SELECT max(id) FROM iam.users), 2, '2026-04-19 10:12:35', 'edit');
INSERT INTO iam.config(id_user, created_at, name, value)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 10:12:35', 'jwt.expire', '15');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-19 10:23:03', '2026-04-19 10:23:13', 'jensjrewlwj', 'aslkahflhew@test.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), null, null);
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 10:23:03', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 10:23:13', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 10:23:26', 'USER', 'LOGIN', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'DELETE_REQ', '');
INSERT INTO iam.tokens(id_user, created_at, expires_at, type, token, payload)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', (CURRENT_TIMESTAMP + INTERVAL '1 day') AT TIME ZONE 'UTC', 'DELETE', 'Z4X9dS6SkaZG12c76FE8s4dzAIVYTecK', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-19 11:41:27', '2026-04-19 11:41:37', 'Admin', 'admin@test.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'en', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), null, null);
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 11:41:27', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-19 11:41:37', 'USER', 'ACTIVATE', '');
INSERT INTO iam.user_permissions(id_user, id_permission, created_at, value)
VALUES ((SELECT max(id) FROM iam.users), 1, '2026-04-19 11:41:27', 'admin');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-22 12:43:41', '2026-04-22 12:43:41', 'Goku11', 'muppet@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Radek', 'Szedro');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 12:43:41', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 12:43:48', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 12:43:53', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-22 14:31:18', '2026-04-22 14:31:28', 'Emilek', 'bajoro@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Emil', 'Bajoro');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 14:31:18', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 14:31:28', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 14:31:38', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-22 20:12:08', '2026-04-22 20:12:27', 'YetAnotherAccount', 'meo3nauydwo@test.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Tomasz', 'Slaciński');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 20:12:08', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 20:12:27', 'USER', 'ACTIVATE', '');
INSERT INTO iam.config(id_user, created_at, name, value)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 20:12:08', 'some.config.name', 'some.config.value');
INSERT INTO iam.config(id_user, created_at, name, value)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 20:12:08', 'other.config.name', 'other.config.value');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-22 21:53:00', '2026-04-22 21:53:00', 'Doge', 'piesio@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Radosław', 'Watacki');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 21:53:00', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 21:53:11', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-22 21:53:26', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-04-26 19:12:08', '2026-04-26 23:44:10', 'Niteczka', 'anna.nitka@example.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Anna', 'Nitka');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-26 19:12:08', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-04-26 23:44:10', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'EMAIL_CHANGE_REQ', '');
INSERT INTO iam.tokens(id_user, created_at, expires_at, type, token, payload)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', (CURRENT_TIMESTAMP + INTERVAL '1 day') AT TIME ZONE 'UTC', 'EMAIL', 'I4X9dS6SkaZG12c76FE8s4dzAIVYTecK', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-05-02 07:26:26', '2026-05-02 07:27:02', 'GrumpyGramps', 'john.doe@example.com', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'en', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'John', 'Doe');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-02 07:26:26', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-02 07:27:02', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'LOGIN', '');
INSERT INTO iam.jwt(id_user, created_at, expires_at, token)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', (CURRENT_TIMESTAMP + INTERVAL '1 day') AT TIME ZONE 'UTC', 'eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiYWRtaW4iLCJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3Nzk4OTIzMjksImV4cCI6MTc3OTk3ODcyOX0.bYvxCevl7FsqN2g9mIoUW0pEqetjvJoIgOrd5T_jmIc');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-05-03 11:12:59', '2026-05-03 12:13:12', 'YoloGuy', 'yolo@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'en', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), null, null);
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-03 11:12:59', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-03 12:13:12', 'USER', 'ACTIVATE', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-05-05 17:12:59', '2026-05-05 17:13:12', 'Ditherino', 'ditherino3@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Not', 'You');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-05 17:12:59', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-05 17:13:12', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'LOGIN', '');
INSERT INTO iam.jwt(id_user, created_at, expires_at, token)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', (CURRENT_TIMESTAMP + INTERVAL '1 day') AT TIME ZONE 'UTC', 'eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiYWRtaW4iLCJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3Nzk4OTIyODksImV4cCI6MTc3OTk3ODY4OX0.bTHFa2yuP92uwBriOAVBRs4YZS3Hjjyk3gH8kg9EArE');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-05-06 08:39:12', '2026-05-06 08:39:12', 'Olek', 'aleksander.romb@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Aleksander', 'Romb');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-06 08:39:12', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-06 08:39:12', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-07 21:47:31', 'USER', 'LOGIN', '');

INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES ('2026-05-06 09:23:56', '2026-05-06 09:24:08', 'Kwiatek', 'kwiatek33@fictional.domain.org', '$2a$10$PyQoDnY93QujZCrw0P5h3u0PmiCfGwKrjvf6.7oYGh8qn.mC4iXZO', 'pl', 'DEMO', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Jolanta', 'Kwiecińska');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-06 09:23:56', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-06 09:24:08', 'USER', 'ACTIVATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), '2026-05-06 09:26:11', 'USER', 'LOGIN', '');

-- ==================
-- Real user: myself.
INSERT INTO iam.users(created_at, modified_at, username, email, password, lang, status, locked)
VALUES (CURRENT_TIMESTAMP AT TIME ZONE 'UTC', CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'Paweł Papierkowski', 'pawel.papierkowski@gmail.com', '$2a$10$hBSXNELF1E/QLpJ/xGHXIeItgrFONEGa.xTk3kurGPFr8bn3N4.Um', 'pl', 'ACTIVE', false);
INSERT INTO iam.profiles(id, name, surname)
VALUES ((SELECT max(id) FROM iam.users), 'Paweł', 'Papierkowski');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'CREATE', '');
INSERT INTO iam.history(id_user, created_at, who, what, params)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'USER', 'ACTIVATE', '');
INSERT INTO iam.user_permissions(id_user, id_permission, created_at, value)
VALUES ((SELECT max(id) FROM iam.users), 1, CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'admin');
INSERT INTO iam.config(id_user, created_at, name, value)
VALUES ((SELECT max(id) FROM iam.users), CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'jwt.expire', '1440');
