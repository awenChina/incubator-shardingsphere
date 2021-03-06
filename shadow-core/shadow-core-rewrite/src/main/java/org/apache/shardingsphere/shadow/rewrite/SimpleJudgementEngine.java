/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shadow.rewrite;

import com.google.common.base.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.rule.ShadowRule;
import org.apache.shardingsphere.sql.parser.relation.segment.insert.InsertValueContext;
import org.apache.shardingsphere.sql.parser.relation.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.relation.statement.impl.InsertSQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.generic.WhereSegmentAvailable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Simple shadow judgement engine.
 *
 * @author zhyee
 */
@RequiredArgsConstructor
public final class SimpleJudgementEngine implements ShadowJudgementEngine {
    
    private final ShadowRule shadowRule;
    
    private final SQLStatementContext sqlStatementContext;
    
    @Override
    public boolean isShadowSQL() {
        if (sqlStatementContext.getSqlStatement() instanceof InsertStatement) {
            for (InsertValueContext each : ((InsertSQLStatementContext) sqlStatementContext).getInsertValueContexts()) {
                if (judgeShadowSqlForInsert(each, (InsertSQLStatementContext) sqlStatementContext)) {
                    return true;
                }
            }
            return false;
        }
        if (sqlStatementContext.getSqlStatement() instanceof WhereSegmentAvailable) {
            Optional<ShadowCondition> shadowCondition = new ShadowConditionEngine(shadowRule).createShadowCondition(sqlStatementContext);
            if (!shadowCondition.isPresent()) {
                return false;
            }
            List<Object> values = shadowCondition.get().getValues(Collections.emptyList());
            return values.size() != 0 && "TRUE".equals(((String) values.get(0)).toUpperCase());
        }
        return false;
    }
    
    private boolean judgeShadowSqlForInsert(final InsertValueContext insertValueContext, final InsertSQLStatementContext insertSQLStatementContext) {
        Iterator<String> descendingColumnNames = insertSQLStatementContext.getDescendingColumnNames();
        while (descendingColumnNames.hasNext()) {
            String columnName = descendingColumnNames.next();
            if (shadowRule.getColumn().equals(columnName)) {
                int columnIndex = insertSQLStatementContext.getColumnNames().indexOf(columnName);
                Object value = insertValueContext.getValue(columnIndex);
                return "TRUE".equals(((String) value).toUpperCase());
            }
        }
        return false;
    }
}
