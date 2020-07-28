package yzw.ahaqth.mycodewarehouse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private static final String dbName = "database.db"; // 数据库名称
    private static final int version = 1; // 数据库版本号

    private static DbHelper dbHelper = null;

    public static void onDestory() {
        dbHelper.close();
        dbHelper = null;
    }

    public static SQLiteDatabase getWriteDB() {
        return dbHelper.getWritableDatabase();
    }

    public static SQLiteDatabase getReadDB() {
        return dbHelper.getReadableDatabase();
    }

    public static void initial(Context context) {
        if (dbHelper == null)
            dbHelper = new DbHelper(context, dbName, null, version);
    }

    private DbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
            case 2:
            case 3:
                /*...*/
        }
    }


    private String getCreateTableSql(Class<?> clazz) {
        String tableName = getDataBaseTableName(clazz);
        StringBuilder builder = new StringBuilder("CREATE TABLE ").append(tableName).append("(");
        List<Field> list = new ArrayList<>();
        getDataBaseFields(clazz, list);
        for (Field field : list) {
            field.setAccessible(true);
            String columnName = getDataBaseColumnName(tableName, field);
            Type type = field.getType();
            if (type == Long.class || type == Long.TYPE
                    || type == Integer.class || type == Integer.TYPE
                    || type == Boolean.class || type == Boolean.TYPE
                    || type == LocalDate.class || type == LocalDateTime.class) {
                builder.append(columnName).append(" INTEGER,");
            } else if (type == String.class) {
                builder.append(columnName).append(" TEXT,");
            } else if (type == Float.class || type == Float.TYPE || type == Double.class || type == Double.TYPE) {
                builder.append(columnName).append(" REAL,");
            }
        }
        return builder.substring(0, builder.length() - 1) + ")";
    }

    public static <T> String getDataBaseTableName(Class<T> clazz) {
        String name = clazz.getName();
        int index = name.lastIndexOf(".") + 1;
        return name.substring(index).toLowerCase();
    }

    public static String getDataBaseColumnName(String tableName, Field field) {
        return tableName + "_" + field.getName().toLowerCase();
    }

    public static <T> Method generateGetMethod(Class<T> clazz, String fieldName) {
        String sb = "get" +
                fieldName.substring(0, 1).toUpperCase() +
                fieldName.substring(1);
        try {
            return clazz.getMethod(sb);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static <T> Method generateSetMethod(Class<T> clazz, String fieldName) {
        try {
            Class<?>[] parameterTypes = new Class<?>[1];
            Field field = clazz.getDeclaredField(fieldName);
            parameterTypes[0] = field.getType();
            String sb = "set" +
                    fieldName.substring(0, 1).toUpperCase() +
                    fieldName.substring(1);
            return clazz.getMethod(sb, parameterTypes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void getDataBaseFields(Class<?> clazz, @NonNull List<Field> resultList) {
        if (clazz == null)
            return;
        Field[] fields1 = clazz.getDeclaredFields();
        for (Field field : fields1) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isPublic(modifiers)
                    || Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers))
                continue;
            field.setAccessible(true);
            resultList.add(field);
        }
        getDataBaseFields(clazz.getSuperclass(), resultList);
    }

    public static <T> T cursor2Modul(Class<T> clazz, Cursor cursor) {
        try {
            String tableName = getDataBaseTableName(clazz);
            T t = clazz.newInstance();
            List<Field> list = new ArrayList<>();
            getDataBaseFields(clazz, list);
            for (Field field : list) {
                field.setAccessible(true);
                Type type = field.getType();
                String columnName = getDataBaseColumnName(tableName, field);

                int index = cursor.getColumnIndex(columnName);
                Object value = null;

                if (type == Long.class || type == Long.TYPE) {
                    value = cursor.getLong(index);
                } else if (type == Integer.class || type == Integer.TYPE) {
                    value = cursor.getInt(index);
                } else if (type == Boolean.class || type == Boolean.TYPE) {
                    value = cursor.getInt(index) != 0;
                } else if (type == Float.class || type == Float.TYPE) {
                    value = cursor.getFloat(index);
                } else if (type == Double.class || type == Double.TYPE) {
                    value = cursor.getDouble(index);
                } else if (type == String.class) {
                    value = cursor.getString(index);
                } else if (type == LocalDate.class) {
                    value = LocalDate.ofEpochDay(cursor.getLong(index));
                } else if (type == LocalDateTime.class) {
                    value = LocalDateTime.ofEpochSecond(cursor.getLong(index), 0, ZoneOffset.ofHours(8));
                }
                field.set(t, value);
            }
            return t;
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> ContentValues modul2ContentValues(Object t) {
        ContentValues contentValues = new ContentValues();
        try {
            List<Field> list = new ArrayList<>();
            getDataBaseFields(t.getClass(), list);
            String tableName = getDataBaseTableName(t.getClass());
            for (Field field : list) {
                field.setAccessible(true);
                Type type = field.getType();
                String columnName = getDataBaseColumnName(tableName,field);

                if (type == Long.class || type == Long.TYPE) {
                    contentValues.put(columnName, field.getLong(t));
                } else if (type == Integer.class || type == Integer.TYPE) {
                    contentValues.put(columnName, field.getInt(t));
                } else if (type == Boolean.class || type == Boolean.TYPE) {
                    int i = field.getBoolean(t) ? 1 : 0;
                    contentValues.put(columnName, i);
                } else if (type == Float.class || type == Float.TYPE) {
                    contentValues.put(columnName, field.getFloat(t));
                } else if (type == Double.class || type == Double.TYPE) {
                    contentValues.put(columnName, field.getDouble(t));
                } else if (type == String.class) {
                    Object o = field.get(t);
                    String s = null == o ? "" : o.toString();
                    contentValues.put(columnName, s);
                } else if (type == LocalDate.class) {
                    long localDate = ((LocalDate) field.get(t)).toEpochDay();
                    contentValues.put(columnName, localDate);
                } else if (type == LocalDateTime.class) {
                    long localDate = ((LocalDateTime) field.get(t)).toEpochSecond(ZoneOffset.ofHours(8));
                    contentValues.put(columnName, localDate);
                }
            }
            return contentValues;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
