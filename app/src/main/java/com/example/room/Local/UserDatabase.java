package com.example.room.Local;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.example.room.Model.User;

import static com.example.room.Local.UserDatabase.DATABSE_VERSION;

@Database(entities = User.class,version = DATABSE_VERSION)
public abstract class UserDatabase extends RoomDatabase {

    public static final int DATABSE_VERSION =1;
    public static final String DATABASE_NAME="Users-Database-Room";

    private static UserDatabase mInstance;

    public abstract UserDAO userDAO();

    public  static UserDatabase getInstance(Context context){

        if (mInstance == null){
            mInstance = Room.databaseBuilder(context,UserDatabase.class,DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                    .build();
        }
        return mInstance;
    }

}
