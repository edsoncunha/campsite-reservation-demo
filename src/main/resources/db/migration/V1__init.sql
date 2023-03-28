create table reservation (
    id                          SERIAL,
    email                       varchar(127) not null,
    checkin                     timestamp not null,
    checkout                    timestamp not null,
    canceled                    boolean not null default false,
    CONSTRAINT reservation_pkey  PRIMARY KEY(id)
);
CREATE INDEX reservation_checkin_idx ON public.reservation (checkin);
CREATE INDEX reservation_checkout_idx ON public.reservation (checkout);