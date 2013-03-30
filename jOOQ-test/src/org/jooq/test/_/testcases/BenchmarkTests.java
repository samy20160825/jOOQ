/**
 * Copyright (c) 2009-2013, Lukas Eder, lukas.eder@gmail.com
 * All rights reserved.
 *
 * This software is licensed to you under the Apache License, Version 2.0
 * (the "License"); You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * . Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * . Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * . Neither the name "jOOQ" nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jooq.test._.testcases;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Random;

import org.jooq.ExecuteListener;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Record6;
import org.jooq.Result;
import org.jooq.Select;
import org.jooq.TableRecord;
import org.jooq.UpdatableRecord;
import org.jooq.impl.Executor;
import org.jooq.test.BaseTest;
import org.jooq.test.jOOQAbstractTest;
import org.jooq.tools.StopWatch;

import org.junit.Test;

public class BenchmarkTests<
    A    extends UpdatableRecord<A> & Record6<Integer, String, String, Date, Integer, ?>,
    AP,
    B    extends UpdatableRecord<B>,
    S    extends UpdatableRecord<S> & Record1<String>,
    B2S  extends UpdatableRecord<B2S> & Record3<String, Integer, Integer>,
    BS   extends UpdatableRecord<BS>,
    L    extends TableRecord<L> & Record2<String, String>,
    X    extends TableRecord<X>,
    DATE extends UpdatableRecord<DATE>,
    BOOL extends UpdatableRecord<BOOL>,
    D    extends UpdatableRecord<D>,
    T    extends UpdatableRecord<T>,
    U    extends TableRecord<U>,
    UU   extends UpdatableRecord<UU>,
    I    extends TableRecord<I>,
    IPK  extends UpdatableRecord<IPK>,
    T725 extends UpdatableRecord<T725>,
    T639 extends UpdatableRecord<T639>,
    T785 extends TableRecord<T785>>
extends BaseTest<A, AP, B, S, B2S, BS, L, X, DATE, BOOL, D, T, U, UU, I, IPK, T725, T639, T785> {

    private static final int    REPETITIONS_NEW_RECORD   = 1000000;
    private static final int    REPETITIONS_RECORD_INTO  = 2000;
    private static final int    REPETITIONS_FIELD_ACCESS = 1000000;
    private static final int    REPETITIONS_SELECT       = 100;
    private static final String RANDOM                   = "" + new Random().nextLong();

    public BenchmarkTests(jOOQAbstractTest<A, AP, B, S, B2S, BS, L, X, DATE, BOOL, D, T, U, UU, I, IPK, T725, T639, T785> delegate) {
        super(delegate);
    }

    @Test
    public void testBenchmarkNewRecord() throws Exception {
        Executor create = create();

        for (int i = 0; i < REPETITIONS_NEW_RECORD; i++) {
            create.newRecord(TBook());
        }
    }

    @Test
    public void testBenchmarkRecordInto() throws Exception {
        Result<B> books = create().fetch(TBook());

        for (int i = 0; i < REPETITIONS_RECORD_INTO; i++) {
            books.into(TBook().getRecordType());
        }
    }

    @Test
    public void testBenchmarkFieldAccess() throws Exception {
        // This benchmark is inspired by a private contribution by Roberto Giacco

        B book = create().newRecord(TBook());

        for (int i = 0; i < REPETITIONS_FIELD_ACCESS; i++) {
            book.setValue(TBook_ID(), i);
            book.setValue(TBook_AUTHOR_ID(), book.getValue(TBook_ID()));

            book.setValue(TBook_PUBLISHED_IN(), i);
            book.setValue(TBook_PUBLISHED_IN(), book.getValue(TBook_PUBLISHED_IN()));
        }
    }

    @Test
    public void testBenchmarkSelect() throws Exception {
        // This benchmark is contributed by "jjYBdx4IL" on GitHub:
        // https://github.com/jOOQ/jOOQ/issues/1625


        Executor create = create();
        create.configuration().getSettings().setExecuteLogging(false);
        create.configuration().setExecuteListeners(Collections.<ExecuteListener>emptyList());

        // Dry-run to avoid side-effects
        testBenchmarkFullExecution(create, 1);
        testBenchmarkReuseSelect(create, 1);
        testBenchmarkReuseSQLString(create, 1);

        // System.in.read();
        StopWatch watch = new StopWatch();
        watch.splitInfo("Benchmark start");

        testBenchmarkFullExecution(create, REPETITIONS_SELECT);
        watch.splitInfo("Full re-execution");

        testBenchmarkReuseSelect(create, REPETITIONS_SELECT);
        watch.splitInfo("Reuse select");

        testBenchmarkReuseSQLString(create, REPETITIONS_SELECT);
        watch.splitInfo("Reuse SQL String");
    }

    private void testBenchmarkReuseSQLString(Executor create, int repetitions) throws Exception {
        String sql = createSelect(create).getSQL(false);
        PreparedStatement pst = getConnection().prepareStatement(sql);
        pst.setLong(1, 1);
        pst.setString(2, RANDOM);

        for (int i = 0; i < repetitions; i++) {
            ResultSet rs = pst.executeQuery();
            create.fetch(rs);
            rs.close();
        }

        pst.close();
    }

    private void testBenchmarkReuseSelect(Executor create, int repetitions) {
        Select<?> scs = createSelect(create);

        for (int i = 0; i < repetitions; i++) {
            scs.execute();
        }
    }

    private void testBenchmarkFullExecution(Executor create, int repetitions) {
        for (int i = 0; i < repetitions; i++) {
            createSelect(create).execute();
        }
    }

    private Select<?> createSelect(Executor create) {
        return create.select()
                     .from(TBook())
                     .where(TBook_ID().equal(1))
                     .and(TBook_TITLE().isNull().or(TBook_TITLE().notEqual(RANDOM)));
    }
}
