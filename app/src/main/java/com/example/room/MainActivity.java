 package com.example.room;

import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.room.Database.UserRepository;
import com.example.room.Local.UserDataSource;
import com.example.room.Local.UserDatabase;
import com.example.room.Model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

 public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private FloatingActionButton floatingActionButton;

    //Adapter

     List<User> userList = new ArrayList<>();
     ArrayAdapter adapter;

     //Database
     private CompositeDisposable compositeDisposable;
     private UserRepository userRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        compositeDisposable = new CompositeDisposable();


        listView = findViewById(R.id.lstUsers);
        floatingActionButton = findViewById(R.id.fab);

        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,userList);
        registerForContextMenu(listView);
        listView.setAdapter(adapter);

        //Database

        UserDatabase userDatabase = UserDatabase.getInstance(this); // create database

        userRepository = UserRepository.getInstance(UserDataSource.getInstance(userDatabase.userDAO()));

        //load all data from database

        loadData();

        //event
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Add new user

                Disposable disposable = Observable.create(new ObservableOnSubscribe<Object>() {

                    @Override
                    public void subscribe(ObservableEmitter<Object> emitter) throws Exception {

                        User user = new User("Hriitk Gupta", UUID.randomUUID().toString()+"@gmail.com");
                        userList.add(user);
                        userRepository.insertUser(user);
                        emitter.onComplete();

                    }
                }).observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                        .subscribe(new Consumer() {

                            @Override
                            public void accept(Object o) throws Exception {
                                Toast.makeText(MainActivity.this, "User Added !!", Toast.LENGTH_SHORT).show();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Toast.makeText(MainActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }, new Action() {
                            @Override
                            public void run() throws Exception {
                                loadData();  //Refresh Data
                            }
                        });


            }
        });



    }

     private void loadData() {
        // Use RxJava

         Disposable disposable = userRepository.getAllUsers()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(new Consumer<List<User>>() {
                                    @Override
                                    public void accept(List<User> users) throws Exception {
                                        onGetAllUserSuccess(users);
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable throwable) throws Exception {
                                        Toast.makeText(MainActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
         compositeDisposable.add(disposable);

     }

     private void onGetAllUserSuccess(List<User> users) {

        userList.clear();
        userList.addAll(users);
        adapter.notifyDataSetChanged();



     }


     @Override
     public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);

        return super.onCreateOptionsMenu(menu);

     }

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){

            case R.id.menu_clear:{
                            deleteAllUser();

                break;
            }


        }

        return super.onOptionsItemSelected(item);
     }

     private void deleteAllUser() {

         Disposable disposable = Observable.create(new ObservableOnSubscribe<Object>() {

             @Override
             public void subscribe(ObservableEmitter<Object> emitter) throws Exception {

                    userRepository.deleteAllUsers();
                 emitter.onComplete();

             }
         }).observeOn(AndroidSchedulers.mainThread())
                 .subscribeOn(Schedulers.io())
                 .subscribe(new Consumer() {

                     @Override
                     public void accept(Object o) throws Exception {
                     }
                 }, new Consumer<Throwable>() {
                     @Override
                     public void accept(Throwable throwable) throws Exception {
                         Toast.makeText(MainActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                     }
                 }, new Action() {
                     @Override
                     public void run() throws Exception {
                         loadData();  //Refresh Data
                     }
                 });

        compositeDisposable.add(disposable);

     }

     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
         super.onCreateContextMenu(menu, v, menuInfo);
         AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
         menu.setHeaderTitle("Select Action");


         menu.add(Menu.NONE,0,Menu.NONE,"Update");
         menu.add(Menu.NONE,1,Menu.NONE,"Delete");

     }

     @Override
     public boolean onContextItemSelected(MenuItem item) {

         AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

         final User user = userList.get(info.position);
         switch (item.getItemId()){

             case 0:{

                 final EditText edtName = new EditText(MainActivity.this);
                 edtName.setText(user.getName());
                 edtName.setHint("Enter your name");
                 new AlertDialog.Builder(MainActivity.this)
                         .setTitle("Edit")
                         .setMessage("Edit user name")
                         .setView(edtName)
                         .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 if (TextUtils.isEmpty(edtName.getText().toString()))
                                     return;
                                 else {
                                     user.setName(edtName.getText().toString());
                                     updateUser(user);
                                 }
                             }
                         }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                    
                         dialog.dismiss();
                         
                     }
                 }).create().show();
                 break;
                 //update
             }

             case 1:{

                 new AlertDialog.Builder(MainActivity.this)

                         .setMessage("Do you want to delete ?"+user.getName())
                         .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                               deleteUser(user);
                             }
                         }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {

                         dialog.dismiss();

                     }
                 }).create().show();

                 break;
                 //delete
             }
         }
            return true;

     }

     private void deleteUser(final User user) {
         Disposable disposable = Observable.create(new ObservableOnSubscribe<Object>() {

             @Override
             public void subscribe(ObservableEmitter<Object> emitter) throws Exception {

                 userRepository.deleteUser(user);
                 emitter.onComplete();

             }
         }).observeOn(AndroidSchedulers.mainThread())
                 .subscribeOn(Schedulers.io())
                 .subscribe(new Consumer() {

                     @Override
                     public void accept(Object o) throws Exception {
                     }
                 }, new Consumer<Throwable>() {
                     @Override
                     public void accept(Throwable throwable) throws Exception {
                         Toast.makeText(MainActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                     }
                 }, new Action() {
                     @Override
                     public void run() throws Exception {
                         loadData();  //Refresh Data
                     }
                 });
         compositeDisposable.add(disposable);
     }

     private void updateUser(final User user) {

         Disposable disposable = Observable.create(new ObservableOnSubscribe<Object>() {

             @Override
             public void subscribe(ObservableEmitter<Object> emitter) throws Exception {

                 userRepository.updateUser(user);
                 emitter.onComplete();

             }
         }).observeOn(AndroidSchedulers.mainThread())
                 .subscribeOn(Schedulers.io())
                 .subscribe(new Consumer() {

                     @Override
                     public void accept(Object o) throws Exception {
                     }
                 }, new Consumer<Throwable>() {
                     @Override
                     public void accept(Throwable throwable) throws Exception {
                         Toast.makeText(MainActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                     }
                 }, new Action() {
                     @Override
                     public void run() throws Exception {
                         loadData();  //Refresh Data
                     }
                 });
         compositeDisposable.add(disposable);

     }

     @Override
     protected void onDestroy() {
         super.onDestroy();
         compositeDisposable.clear();
     }
 }
