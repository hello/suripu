
INSERT INTO accounts (firstname,lastname,username,email,password_hash,tz)
VALUES('test firstname','test lastname','', 'test@sayhello.com', '$2a$12$bz/aglNZwmUzdJCrhXsm4.lRqNXI7SDzr/AkQlH8RkCLnznyCo9/m', 'America/Los_Angeles');


INSERT INTO oauth_applications (name, client_id, client_secret, redirect_uri, scopes, dev_account_id,grant_type)
VALUES('Local test app','local_client_id','local_client_secret','http://hello.co', '{1,2,3,4,5,6,7,8,9}', 1, 2);