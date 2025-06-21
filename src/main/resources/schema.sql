-- イベントテーブル
CREATE TABLE IF NOT EXISTS public.event (
  event_id serial PRIMARY KEY,
  start_time time without time zone,
  end_time time without time zone,
  lat double precision,
  lng double precision,
  user_id integer,
  event_date date,
  event_title varchar(50),
  recruit_min integer,
  recruit_max integer,
  com_method varchar(50),
  event_type varchar(10)
);

-- コメントテーブル
CREATE TABLE IF NOT EXISTS public.event_comments (
  comment_id serial PRIMARY KEY,
  event_id integer NOT NULL,
  user_id integer NOT NULL,
  comment_text text NOT NULL,
  comment_time timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- イベント詳細
CREATE TABLE IF NOT EXISTS public.event_details (
  event_id integer PRIMARY KEY,
  event_message text
);

-- イベント参加者
CREATE TABLE IF NOT EXISTS public.event_participants (
  event_id integer NOT NULL,
  user_id integer NOT NULL,
  PRIMARY KEY (event_id, user_id)
);

-- イベント制限条件
CREATE TABLE IF NOT EXISTS public.restriction (
  event_id integer,
  restriction integer
);

-- ユーザー性別
CREATE TABLE IF NOT EXISTS public.user_gender (
  user_id integer PRIMARY KEY,
  user_gender varchar(10)
);

-- ユーザーメール
CREATE TABLE IF NOT EXISTS public.user_mail (
  user_id integer PRIMARY KEY,
  email varchar(255) NOT NULL
);

-- ユーザー名
CREATE TABLE IF NOT EXISTS public.user_name (
  user_id serial PRIMARY KEY,
  user_name varchar(20)
);

-- パスワード
CREATE TABLE IF NOT EXISTS public.user_password (
  user_id integer PRIMARY KEY,
  hashed_password varchar(255) NOT NULL
);

-- プロフィール
CREATE TABLE IF NOT EXISTS public.user_profile (
  user_id integer PRIMARY KEY,
  user_profile text
);
