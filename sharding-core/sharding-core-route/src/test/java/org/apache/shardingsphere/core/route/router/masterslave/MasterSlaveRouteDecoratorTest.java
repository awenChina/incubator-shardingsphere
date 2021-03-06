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

package org.apache.shardingsphere.core.route.router.masterslave;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.shardingsphere.core.route.RouteResult;
import org.apache.shardingsphere.core.route.ShardingRouteResult;
import org.apache.shardingsphere.core.route.type.RoutingResult;
import org.apache.shardingsphere.core.route.type.RoutingUnit;
import org.apache.shardingsphere.core.route.type.TableUnit;
import org.apache.shardingsphere.core.rule.MasterSlaveRule;
import org.apache.shardingsphere.core.strategy.masterslave.RandomMasterSlaveLoadBalanceAlgorithm;
import org.apache.shardingsphere.sql.parser.relation.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.LockSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class MasterSlaveRouteDecoratorTest {
    
    private static final String DATASOURCE_NAME = "ds";
    
    private static final String NON_MASTER_SLAVE_DATASOURCE_NAME = "nonMsDatasource";
    
    private static final String MASTER_DATASOURCE = "master";
    
    private static final String SLAVE_DATASOURCE = "query";
    
    @Mock
    private MasterSlaveRule masterSlaveRule;
    
    @Mock
    private SQLStatementContext sqlStatementContext;
    
    @Mock
    private InsertStatement insertStatement;
    
    @Mock
    private SelectStatement selectStatement;
    
    private MasterSlaveRouteDecorator routeDecorator;
    
    @Before
    public void setUp() {
        routeDecorator = new MasterSlaveRouteDecorator(masterSlaveRule);
        when(masterSlaveRule.getName()).thenReturn(DATASOURCE_NAME);
        when(masterSlaveRule.getMasterDataSourceName()).thenReturn(MASTER_DATASOURCE);
        when(masterSlaveRule.getSlaveDataSourceNames()).thenReturn(Lists.newArrayList(SLAVE_DATASOURCE));
        when(masterSlaveRule.getLoadBalanceAlgorithm()).thenReturn(new RandomMasterSlaveLoadBalanceAlgorithm());
    }
    
    @After
    public void tearDown() {
        MasterVisitedManager.clear();
    }
    
    @Test
    public void assertDecorateToMaster() {
        RouteResult routeResult = mockSQLRouteResult(insertStatement);
        RouteResult actual = routeDecorator.decorate(routeResult);
        Iterator<String> routedDataSourceNames = actual.getRoutingResult().getDataSourceNames().iterator();
        assertThat(routedDataSourceNames.next(), is(NON_MASTER_SLAVE_DATASOURCE_NAME));
        assertThat(routedDataSourceNames.next(), is(MASTER_DATASOURCE));
    }
    
    @Test
    public void assertDecorateToSlave() {
        RouteResult routeResult = mockSQLRouteResult(selectStatement);
        when(selectStatement.getLock()).thenReturn(Optional.<LockSegment>absent());
        RouteResult actual = routeDecorator.decorate(routeResult);
        Iterator<String> routedDataSourceNames = actual.getRoutingResult().getDataSourceNames().iterator();
        assertThat(routedDataSourceNames.next(), is(NON_MASTER_SLAVE_DATASOURCE_NAME));
        assertThat(routedDataSourceNames.next(), is(SLAVE_DATASOURCE));
    }

    @Test
    public void assertLockDecorateToMaster() {
        RouteResult routeResult = mockSQLRouteResult(selectStatement);
        when(selectStatement.getLock()).thenReturn(Optional.of(mock(LockSegment.class)));
        RouteResult actual = routeDecorator.decorate(routeResult);
        Iterator<String> routedDataSourceNames = actual.getRoutingResult().getDataSourceNames().iterator();
        assertThat(routedDataSourceNames.next(), is(NON_MASTER_SLAVE_DATASOURCE_NAME));
        assertThat(routedDataSourceNames.next(), is(MASTER_DATASOURCE));
    }
    
    private ShardingRouteResult mockSQLRouteResult(final SQLStatement sqlStatement) {
        when(sqlStatementContext.getSqlStatement()).thenReturn(sqlStatement);
        ShardingRouteResult result = new ShardingRouteResult(sqlStatementContext, null, null);
        result.setRoutingResult(mockRoutingResult());
        return result;
    }
    
    private RoutingResult mockRoutingResult() {
        RoutingResult result = new RoutingResult();
        RoutingUnit routingUnit = new RoutingUnit(DATASOURCE_NAME);
        routingUnit.getTableUnits().add(new TableUnit("table", "table_0"));
        result.getRoutingUnits().add(routingUnit);
        result.getRoutingUnits().add(new RoutingUnit(NON_MASTER_SLAVE_DATASOURCE_NAME));
        return result;
    }
}
