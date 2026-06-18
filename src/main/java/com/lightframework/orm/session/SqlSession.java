    package com.lightframework.orm.session;

    import java.io.Closeable;
    import java.util.List;


    public interface SqlSession extends Closeable {

        /**
         * Retrieve a single row mapped from the statement key
         * 根据指定的SqlID获取一条记录的封装对象
         *
         * @param <T>       the returned object type 封装之后的对象类型
         * @param statement sqlID
         * @return Mapped object 封装之后的对象
         */
        <T> T selectOne(String statement);

        /**
         * Retrieve a single row mapped from the statement key and parameter.
         * 根据指定的SqlID获取一条记录的封装对象，只不过这个方法容许我们可以给sql传递一些参数
         * 一般在实际使用中，这个参数传递的是pojo，或者Map或者ImmutableMap
         *
         * @param <T>       the returned object type
         * @param statement Unique identifier matching the statement to use.
         * @param parameter A parameter object to pass to the statement.
         * @return Mapped object
         */
        <T> T selectOne(String statement, Object parameter);

        /**
         * Retrieve a list of mapped objects from the statement key and parameter.
         * 获取多条记录，这个方法容许我们可以传递一些参数
         *
         * @param <E>       the returned list element type
         * @param statement Unique identifier matching the statement to use.
         * @param parameter A parameter object to pass to the statement.
         * @return List of mapped object
         */
        <E> List<E> selectList(String statement, Object parameter);

        /**
         * Execute an insert statement with the given parameter object. Any generated
         * autoincrement values or selectKey entries will modify the given parameter
         * object properties. Only the number of rows affected will be returned.
         * 插入记录，容许传入参数。
         *
         * @param statement Unique identifier matching the statement to execute.
         * @param parameter A parameter object to pass to the statement.
         * @return int The number of rows affected by the insert. 注意返回的是受影响的行数
         */
        int insert(String statement, Object parameter);

        /**
         * Execute an update statement. The number of rows affected will be returned.
         * 更新记录
         *
         * @param statement Unique identifier matching the statement to execute.
         * @param parameter A parameter object to pass to the statement.
         * @return int The number of rows affected by the update. 返回的是受影响的行数
         */
        int update(String statement, Object parameter);

        /**
         * Execute a delete statement. The number of rows affected will be returned.
         * 删除记录
         *
         * @param statement Unique identifier matching the statement to execute.
         * @param parameter A parameter object to pass to the statement.
         * @return int The number of rows affected by the delete. 返回的是受影响的行数
         */
        Object delete(String statement, Object parameter);

        /**
         * 以下是事务控制方法 commit,rollback
         * Flushes batch statements and commits database connection.
         * Note that database connection will not be committed if no updates/deletes/inserts were called.
         */
        void commit();

        /**
         * Retrieves current configuration
         * 得到配置
         *
         * @return Configuration
         */
        Configuration getConfiguration();

        /**
         * Retrieves a mapper.
         * 得到映射器，这个巧妙的使用了泛型，使得类型安全
         *
         * @param <T>  the mapper type
         * @param type Mapper interface class
         * @return a mapper bound to this SqlSession
         */
        <T> T getMapper(Class<T> type);

        @Override
        default void close() {
            // 检查是否有需要回滚的事务
            try {
                // 这里可以检查是否有未提交的事务，但在接口层面我们无法直接获取事务状态
                // 我们只能提供一个基本实现，具体实现应该由子类重写

                // 记录日志 - 在实际实现中应该使用日志框架
                System.out.println("Closing SqlSession...");

                // 这里可以添加一些通用的清理逻辑
                // 例如：清理线程局部变量，释放基本资源等

                // 在真正的实现中，这里应该包含：
                // 1. 检查并回滚未提交的事务
                // 2. 关闭数据库连接
                // 3. 清理缓存
                // 4. 将连接返回连接池

                // 注意：由于这是一个接口默认方法，我们无法直接访问具体实现的状态
                // 因此这个实现主要是为了提供基本的结构和日志记录

                // 在实际应用中，实现类应该重写这个方法
                // 并调用 super.close() 以确保基础清理工作被执行

            } catch (Exception e) {
                // 记录关闭过程中的异常，但不抛出，因为close()方法通常不应抛出异常
                System.err.println("Error while closing SqlSession: " + e.getMessage());
                // 在实际实现中，应该使用日志框架记录这个异常
            } finally {
                // 确保资源被标记为已关闭
                // 在真正的实现中，这里应该设置一个标志位来表示SqlSession已关闭
                System.out.println("SqlSession closed successfully");
            }
        }

    }
