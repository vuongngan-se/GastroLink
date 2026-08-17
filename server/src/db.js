const mysql = require("mysql2/promise");

let pool = null;
let isDbAvailable = false;

async function initDb() {
  const host = process.env.DB_HOST || "localhost";
  const user = process.env.DB_USER || "root";
  const password = process.env.DB_PASSWORD || "";
  const database = process.env.DB_NAME || "gastrolink";

  try {
    // 1. Try to connect to MySQL server to create the database if not exists
    const tempConnection = await mysql.createConnection({ host, user, password });
    await tempConnection.query(`CREATE DATABASE IF NOT EXISTS \`${database}\`;`);
    await tempConnection.end();

    // 2. Create connection pool
    pool = mysql.createPool({
      host,
      user,
      password,
      database,
      waitForConnections: true,
      connectionLimit: 5,
      queueLimit: 0
    });

    // 3. Create tables if they do not exist
    const conn = await pool.getConnection();
    try {
      await conn.query(`
        CREATE TABLE IF NOT EXISTS user_profiles (
          userId VARCHAR(50) NOT NULL PRIMARY KEY,
          age INT,
          sex VARCHAR(15),
          weight DOUBLE,
          height DOUBLE,
          goal VARCHAR(30),
          activityLevel VARCHAR(20),
          allergies TEXT,
          savedProfilesJson TEXT,
          updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
      `);

      try {
        await conn.query("ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS savedProfilesJson TEXT;");
      } catch (_) {}
      try {
        await conn.query("ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS sex VARCHAR(15);");
      } catch (_) {}
      try {
        await conn.query("ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS activityLevel VARCHAR(20);");
      } catch (_) {}

      await conn.query(`
        CREATE TABLE IF NOT EXISTS user_orders (
          orderId VARCHAR(50) NOT NULL PRIMARY KEY,
          userId VARCHAR(50) NOT NULL,
          orderDate VARCHAR(50),
          totalKcal INT,
          totalProtein INT,
          totalCarbs INT,
          totalFat INT,
          itemsJson TEXT,
          updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          KEY idx_user_id (userId)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
      `);

      isDbAvailable = true;
      console.log("🟢 Kết nối MySQL thành công và các bảng đã được khởi tạo!");
    } finally {
      conn.release();
    }
  } catch (error) {
    console.warn("⚠️ Không thể kết nối với MySQL Database. Tính năng đồng bộ đám mây sẽ chạy ở chế độ dự phòng.");
    console.warn(`Lỗi chi tiết: ${error.message}`);
    isDbAvailable = false;
  }
}

async function saveProfile(profile) {
  if (!isDbAvailable) return false;
  const sql = `
    INSERT INTO user_profiles (userId, age, sex, weight, height, goal, activityLevel, allergies, savedProfilesJson)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON DUPLICATE KEY UPDATE
      age = VALUES(age),
      sex = VALUES(sex),
      weight = VALUES(weight),
      height = VALUES(height),
      goal = VALUES(goal),
      activityLevel = VALUES(activityLevel),
      allergies = VALUES(allergies),
      savedProfilesJson = VALUES(savedProfilesJson);
  `;
  await pool.query(sql, [
    profile.userId || "demo_user",
    profile.age || null,
    profile.sex || null,
    profile.weight || null,
    profile.height || null,
    profile.goal || null,
    profile.activityLevel || null,
    profile.allergies || "",
    profile.savedProfilesJson || "[]"
  ]);
  return true;
}

async function getProfile(userId) {
  if (!isDbAvailable) return null;
  const [rows] = await pool.query("SELECT * FROM user_profiles WHERE userId = ?", [userId || "demo_user"]);
  return rows[0] || null;
}

async function saveOrders(userId, orders) {
  if (!isDbAvailable) return false;
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    for (const order of orders) {
      const sql = `
        INSERT INTO user_orders (orderId, userId, orderDate, totalKcal, totalProtein, totalCarbs, totalFat, itemsJson)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          orderDate = VALUES(orderDate),
          totalKcal = VALUES(totalKcal),
          totalProtein = VALUES(totalProtein),
          totalCarbs = VALUES(totalCarbs),
          totalFat = VALUES(totalFat),
          itemsJson = VALUES(itemsJson);
      `;
      await conn.query(sql, [
        order.orderId,
        userId || "demo_user",
        order.orderDate,
        order.totalKcal,
        order.totalProtein,
        order.totalCarbs,
        order.totalFat,
        JSON.stringify(order.items || [])
      ]);
    }
    await conn.commit();
    return true;
  } catch (error) {
    await conn.rollback();
    throw error;
  } finally {
    conn.release();
  }
}

async function getOrders(userId) {
  if (!isDbAvailable) return [];
  const [rows] = await pool.query("SELECT * FROM user_orders WHERE userId = ? ORDER BY orderDate DESC", [userId || "demo_user"]);
  return rows.map(r => ({
    orderId: r.orderId,
    orderDate: r.orderDate,
    totalKcal: r.totalKcal,
    totalProtein: r.totalProtein,
    totalCarbs: r.totalCarbs,
    totalFat: r.totalFat,
    items: JSON.parse(r.itemsJson || "[]")
  }));
}

module.exports = {
  initDb,
  saveProfile,
  getProfile,
  saveOrders,
  getOrders,
  isDbAvailable: () => isDbAvailable
};
