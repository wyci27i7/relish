package cn.edu.scut.common.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class InsertUsers {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://172.24.195.136:3306/relish";
        String username = "dev";
        String password = "123456";

        Connection conn = DriverManager.getConnection(url, username, password);
        String sql = "INSERT INTO tb_user (phone, nick_name) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);

        for (int i = 1; i <= 200; i++) {
            String phone = String.format("138%08d", i);
            pstmt.setString(1, phone);
            pstmt.setString(2, "user_" + i);
            pstmt.addBatch();
        }

        pstmt.executeBatch();
        System.out.println("成功插入200个用户");

        pstmt.close();
        conn.close();
    }
}
