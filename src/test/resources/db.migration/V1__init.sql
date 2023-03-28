create table reservation (
    id                          SERIAL,
    email                       varchar(127) not null,
    checkin                     timestamp with timezone not null,
    checkout                    timestamp with timezone not null,
    canceled                    boolean default false,
    CONSTRAINT reservation_pkey primary key(id)
);
CREATE INDEX reservation_checkin_idx ON public.reservation (checkin);
CREATE INDEX reservation_checkout_idx ON public.reservation (checkout);