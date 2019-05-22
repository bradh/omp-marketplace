#!/usr/bin/env bash

sqlplus -S omp/omp@localhost/oracle > oracle-new.sql \
<<-EOSQL
    set pagesize 0;
    SET LINESIZE 200;
    SET LONG 10000;
    SET LONGCHUNKSIZE 10000;
    set feedback off;
    set echo off;

    EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'PRETTY', true);
    EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', true);
    EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS_AS_ALTER', true);
    EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SIZE_BYTE_KEYWORD', true);
    EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'STORAGE', false);
    EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_ATTRIBUTES', false);

    SELECT DBMS_METADATA.GET_DDL('TABLE', u.table_name) FROM USER_TABLES u ORDER BY table_name;

    SELECT DBMS_METADATA.GET_DDL('INDEX', u.index_name) FROM USER_INDEXES u ORDER BY index_name;

    SELECT DBMS_METADATA.GET_DDL('SEQUENCE', u.sequence_name) FROM USER_SEQUENCES u ORDER BY sequence_name;

    exit;
EOSQL
