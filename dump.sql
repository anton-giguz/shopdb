--
-- PostgreSQL database dump
--

-- Dumped from database version 12.2
-- Dumped by pg_dump version 12.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.purchases DROP CONSTRAINT IF EXISTS purchases_product_fkey;
ALTER TABLE IF EXISTS ONLY public.purchases DROP CONSTRAINT IF EXISTS purchases_customer_fkey;
ALTER TABLE IF EXISTS ONLY public.purchases DROP CONSTRAINT IF EXISTS purchases_pkey;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS products_pkey;
ALTER TABLE IF EXISTS ONLY public.customers DROP CONSTRAINT IF EXISTS customers_pkey;
ALTER TABLE IF EXISTS public.purchases ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.products ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.customers ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS public.purchases_id_seq;
DROP TABLE IF EXISTS public.purchases;
DROP SEQUENCE IF EXISTS public.products_id_seq;
DROP TABLE IF EXISTS public.products;
DROP SEQUENCE IF EXISTS public.customers_id_seq;
DROP TABLE IF EXISTS public.customers;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id integer NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL
);


--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customers_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id integer NOT NULL,
    name text NOT NULL,
    price numeric NOT NULL
);


--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.products_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: purchases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.purchases (
    id integer NOT NULL,
    customer integer NOT NULL,
    product integer NOT NULL,
    purchase_date date NOT NULL
);


--
-- Name: purchases_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.purchases_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: purchases_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.purchases_id_seq OWNED BY public.purchases.id;


--
-- Name: customers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers ALTER COLUMN id SET DEFAULT nextval('public.customers_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: purchases id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases ALTER COLUMN id SET DEFAULT nextval('public.purchases_id_seq'::regclass);


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (id, first_name, last_name) FROM stdin;
1	Антон	Иванов
2	Николай	Иванов
3	Валентин	Петров
4	Полина	Смирнова
5	Дарья	Кузнецова
6	Николай	Никитин
7	Анастасия	Фролова
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.products (id, name, price) FROM stdin;
1	Хлеб	25
2	Молоко	40
3	Сыр	66
4	Сметана	57
5	Колбаса	101
6	Минеральная вода	24
\.


--
-- Data for Name: purchases; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.purchases (id, customer, product, purchase_date) FROM stdin;
1	1	1	2020-01-14
2	1	1	2020-01-20
3	1	1	2020-01-25
4	1	2	2020-01-19
5	1	4	2020-01-17
6	1	5	2020-01-24
7	2	3	2020-01-15
8	2	3	2020-01-22
9	3	1	2020-01-21
10	3	3	2020-01-21
11	3	6	2020-01-15
12	3	6	2020-01-13
13	4	1	2020-01-14
14	5	1	2020-01-15
15	5	1	2020-01-24
16	5	2	2020-01-15
17	5	2	2020-01-24
18	7	3	2020-01-17
19	7	3	2020-01-22
20	7	5	2020-01-16
\.


--
-- Name: customers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customers_id_seq', 7, true);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.products_id_seq', 6, true);


--
-- Name: purchases_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.purchases_id_seq', 20, true);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: purchases purchases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_pkey PRIMARY KEY (id);


--
-- Name: purchases purchases_customer_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_customer_fkey FOREIGN KEY (customer) REFERENCES public.customers(id);


--
-- Name: purchases purchases_product_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.purchases
    ADD CONSTRAINT purchases_product_fkey FOREIGN KEY (product) REFERENCES public.products(id);


--
-- PostgreSQL database dump complete
--

