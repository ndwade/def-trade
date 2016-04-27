--
-- def-trade schema
-- 
-- note on varchar: 
-- - for short identifier-like entries, using varchar(16). Exact length insufficient for validation of e.g. ISIN.
-- - Postgres is more efficient with varchar less than 126
-- - longer? use 'text' type.
--
drop type if exists sec_type_e cascade;
create type sec_type_e AS enum (
  'stock',
  'future',
  'option',
  'bond',
  'cash',
  'error'
);

drop type if exists rf_statement_type_e cascade;
create type rf_statement_type_e AS enum (
  'income',
  'balance_sheet',
  'cash_flow',
  'error'
); -- for COA codes

drop table if exists currencies cascade;
create table currencies (
  symbol char(3) primary key,
  country_code char(2),
  description varchar(126)	-- informal name (e.g. "Swiss Franc" for CHF)
);

drop table if exists refd_options;
create table refd_options();

/*
 * exchange data per ISO 10383
 * point in time strategy:
 * every mod results in a new row
 * daily scrub of contracts which point to stale exchange id
 * (this won't be used yet - placeholder)
 */
/*
drop table if exists iso_exchanges cascade;
create table iso_exchanges (
  id serial4 primary key,
  mic char(4) not null,
  omic char(4) not null,
  o_or_s exchange_os_e not null,
  acronym varchar(126),
  name varchar(126) not null,
  city varchar(126) not null,
  url varchar(126),
  status_ts timestamp not null,
  end_ts timestamp	-- active if null
);
create index iso_exchanges_mic_index on iso_exchanges (mic);
*/

/*
 * IB specific exchange names - 
 * no clear way to unify with ISO 10383 data set,
 * so just use IB specific names
 */
drop table if exists exchanges cascade;
create table exchanges (

  id serial4 primary key,

  name varchar(126) not null,
  
  status_ts timestamp not null,
  end_ts timestamp	-- active if null
);

/*
 * validation of identifiers: need to deal with checksums etc; no sense in simply limiting char length.
 * point in time strategy: new row on every mod; never delete. Forward and backward pointers.
 */
drop table if exists corporations cascade;
create table corporations (
  
  id serial4 primary key,
  name varchar(126) not null,
  
  meta jsonb, -- IRS_no, rep_no, Reuters Fundamentals DB index, CIK_no - SEC Central Index Key - etc
  
  status_ts timestamp not null,
  end_ts timestamp -- null means current
);

-- P.I.T strategy: assuming that contracts are immutable for a given con_id
-- new identifiers issued after material corporate actions
drop table if exists contracts cascade;
create table contracts (
  
  con_id int4 primary key,	-- yes - but - can have several different con_id for the same isin...
  
  symbol varchar(16) not null,
  sec_type sec_type_e not null,
  local_symbol varchar(126), -- could be long for derivatives; why worry
  exchange_id int4 not null references exchanges (id),
  currency char(3) not null references currencies (symbol),
  meta jsonb, -- cusip, isin, sedol, ric, etc...
  
  corp_id int4 references corporations (id), -- note: nullable. Make association through reqFundamentalData API call if possible
  unique(symbol, exchange_id)
);

/*
 * market data
 */

drop table if exists md_vendors cascade;
create table md_vendors (

  id serial4 primary key,
  
  name varchar(126) unique not null,
  description text,
  
  status_ts timestamp not null,
  end_ts timestamp -- null means current
);

-- a market data feed corresponds directly to an api identifier of some kind.
drop table if exists md_feeds cascade;
create table md_feeds (

  id serial4 primary key,
  vendor_id int4 not null references md_vendors(id),

  name varchar(126) unique not null, -- assigned by maintainer, for reporting, e.g. 'IB US equities consolidated'
  meta jsonb, -- feed-specific details for subscribing
    
  status_ts timestamp not null,
  end_ts timestamp -- null means current
);

-- join table feeds <--> exchanges
drop table if exists md_feeds_exchanges cascade;
create table md_feeds_exchanges (

  feed_id int4 not null references md_feeds(id) on delete restrict,	-- P.I.T., remember?,
  exchange_id int4 not null references exchanges(id) on delete restrict,

  sec_types sec_type_e[] not null,
  
  status_ts timestamp not null, -- a feed may drop or add an exchange
  end_ts timestamp -- null means current
);

-- root metadata for all data series
-- intended to capture reproducability of the processed data series
-- used for both row and processed tables
drop table if exists ds_root cascade;
create table ds_root (
  id serial4 primary key,
  ts timestamp, -- when added
  meta jsonb not null, -- project specific - e.g GenericTickList or whatToShow for raw market data series
  encoding jsonb not null -- something like { name: string, repo: url, commit: string, project: string, decoder: fqnClassName, processor fqnClassName }
);

-- each data series specifies its inputs with this table
-- in principle every series can be created from raw data (no listed sources)
drop table if exists ds_inputs cascade;
create table ds_inputs (
  ds_id int4 not null references ds_root(id) on delete cascade,
  input_id int4  not null references ds_root(id) on delete cascade,
  primary key (ds_id, input_id)
);

-- raw market data feed logs.
-- ds_id's specified here should _not_ have any inputs
drop table if exists md_series cascade;
create table md_series (

  ds_id int4 not null references ds_root(id),
  
  con_id int4 not null references contracts (con_id),
  exchange_id int4 not null references exchanges (id),			-- these may be very different
  currency char(3) not null references currencies (symbol),     -- e.g. Apple trading in Frankfurt in Euros
  
  feed_id int4 references md_feeds(id) on delete restrict,
  
  period interval not null,	-- bar size for bars, peck size for ticks
  
  start_ts timestamp not null,	-- note - this is the reported *exchange* time, not the time on our machines (some ms later)
  end_ts timestamp -- null means not finished yet!
);
create index md_series_con_id_index on md_series (con_id, exchange_id, currency);

-- ds_pecks: holds packed, pickled ticks.
-- also holds high frequency bar data (e.g. 5 second bars)
-- also holds random, specialed stuff (like high freq greeks bars for options)
-- note: this table holds market data *as collected* (with possible conservative scrub - noted in md_series.encoding field)
-- corrected or filtered data becomes historical data or processed data
drop table if exists ds_pecks cascade;
create table ds_pecks (
  ds_id int4 not null references ds_root(id) on delete restrict, -- n.b. no primary key! - would be a waste of 8 bytes
  start timestamp not null, -- exact semantics dependent on series info
  peck jsonb not null
);
create index ds_pecks_ts_index on ds_pecks (start);

-- use explicit bars for lower frequency data (e.g. hours/days/months/years)
drop table if exists ds_bars cascade;
create table ds_bars (

  ds_id int4 not null references ds_root(id) on delete cascade,
  start timestamp not null,	-- note - this is the reported *exchange* time, not the time on our machines (some ms later)
  
  "open" float8 not null,
  "high" float8 not null,
  "low" float8 not null,
  "close" float8 not null,
  "volume" int4 not null,	-- remember the bar could be a year or even a decade
  "count" int4, -- can be null - only valid for TRADES 
  "wap" float8 not null,
  has_gaps boolean not null
  
);


/*
 * Reuters Fundamentals tables
 */

-- COA -> Chart of Accounts
-- paranoid - what if COA codes get reused when they change?
-- use point in time strategy
drop table if exists rf_coa_code cascade;
create table rf_coa_code (
  id serial4 primary key,
  code char(6) not null,	
  rf_statement_type rf_statement_type_e not null,
  description text not null,
  status_ts timestamp not null,
  end_ts timestamp -- null means current
);

-- use point in time strategy on statements
drop table if exists rf_statements cascade;
create table rf_statements (
  
  id serial4 primary key,
  corp_id int4 not null references corporations (id),
  
  rf_statement_type rf_statement_type_e not null,
  period_length int2 not null,
  period_type varchar(126) not null, -- Annual, Quarterly - others?!
  statement_date date not null,
  meta jsonb, -- source, update_type, auditor, opinion, etc...
  
  status_ts timestamp not null,
  end_ts timestamp -- null means current
);

-- don't need point-in-time dates in line items table:
-- since each line item references a statement, and a new statement is added
-- when restated, dates on statements are sufficient - superceded statement will record end_ts
drop table if exists rf_line_items cascade;
create table rf_line_items (
  coa_id int4 not null references rf_coa_code (id),
  statement_id int4 not null references rf_statements (id),
  value decimal not null
);
