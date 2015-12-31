package com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model;

import java.util.List;

import org.springframework.util.StringUtils;

import com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.SqlExpression;
import com.google.common.collect.Lists;

/**
 * OrderBy
 * 
 * @author luowenlei
 *
 */
public class OrderBy extends SqlSegment {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 6181293551792603881L;
    
    /**
     * selectList
     */
    private List<SqlColumn> orderByList = Lists.newArrayList();

    /* (non-Javadoc)
     * @see com.baidu.rigel.biplatform.queryrouter.queryplugin.sql.model.SqlSegment#getSql()
     */
    @Override
    public String getSql() {
        // TODO Auto-generated method stub
        if (orderByList.isEmpty()) {
            return "";
        }
        String sql = "";
        for (SqlColumn sqlColumn : orderByList) {
            if (sqlColumn.getSortRecord() == null) {
                continue;
            }
            String orderByType = "";
            if (SqlConstants.DESC.equals(sqlColumn.getSortRecord().getSortType().name())) {
                orderByType = SqlConstants.DESC;
            }
            if (StringUtils.isEmpty(sql)) {
                sql = " order by ";
            }
            sql = sql + SqlExpression.getSqlColumnName(sqlColumn, this.isHasAlias()) + SqlConstants.SPACE
                    + orderByType + SqlConstants.SPACE + SqlConstants.COMMA;
        }
        if (StringUtils.isEmpty(sql)) {
            return "";
        } else {
            sql = sql.toString().substring(0, sql.toString().lastIndexOf(SqlConstants.COMMA))
                    + SqlConstants.SPACE;
            sql = StringUtils.replace(sql, "  ", " ");
            sql = StringUtils.replace(sql, "  ", " ");
            return sql;
        }
    }

    /**
     * default generate get orderByList
     * @return the orderByList
     */
    public List<SqlColumn> getOrderByList() {
        return orderByList;
    }

    public OrderBy(boolean hasAlias) {
        super(hasAlias);
        // TODO Auto-generated constructor stub
    }
}
