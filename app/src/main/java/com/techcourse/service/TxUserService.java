package com.techcourse.service;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.CannotGetJdbcConnectionException;
import com.interface21.jdbc.datasource.DataSourceUtils;
import com.interface21.transaction.support.TransactionSynchronizationManager;
import com.techcourse.domain.User;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxUserService implements UserService {

    private static final Logger log = LoggerFactory.getLogger(TxUserService.class);

    private final UserService userService;
    private final DataSource dataSource;

    public TxUserService(final UserService userService, final DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    @Override
    public User findById(final long id) {
        return userService.findById(id);
    }

    @Override
    public void save(final User user) {
        userService.save(user);
    }

    @Override
    public void changePassword(final long id, final String newPassword, final String createdBy) {
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);

            try {
                connection.setAutoCommit(false);
                userService.changePassword(id, newPassword, createdBy);
                connection.commit();
            } catch (Exception exceptionWhenCommit) {
                log.error("커밋 실패", exceptionWhenCommit);

                try {
                    connection.rollback();
                    throw new DataAccessException("커밋 중 에러가 발생했습니다. 롤백합니다.", exceptionWhenCommit);
                } catch (SQLException exceptionWhenRollback) {
                    log.error("롤백 실패", exceptionWhenRollback);
                    throw new DataAccessException("롤백 중 에러가 발생했습니다.", exceptionWhenRollback);
                }
            }
        } catch (CannotGetJdbcConnectionException exceptionWhenConnect) {
            log.error("DB 커넥션 획득 실패.", exceptionWhenConnect);
            throw new DataAccessException("커넥션을 얻지 못했습니다.", exceptionWhenConnect);
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
                TransactionSynchronizationManager.unbindResource(dataSource);
            }
        }
    }
}
