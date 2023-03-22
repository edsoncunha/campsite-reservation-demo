create table reservation (
    id                          SERIAL,
    email                       varchar(127),
    checkin                     timestamp with timezone,
    checkout                    timestamp with timezone,
    canceled                    boolean
);
CREATE INDEX reservation_checkin_idx ON public.reservation (checkin);
CREATE INDEX reservation_checkout_idx ON public.reservation (checkout);