package com.example.jihyun.oingoing;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;

import static android.R.id.progress;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    ArrayAdapter adapter;
    ProgressBar ProgressBar;
    final static String LOG_TAG = "myLogs";
    private static int id = 1;
    FloatingActionButton fab1, fab2, fab3, fab4;
    private Realm myRealm;
    private ListView lvPersonNameList;
    private static ArrayList<DataDetailsModel> dataDetailsModelArrayList = new ArrayList<>();
    private DataDetailsAdapter dataDetailsAdapter;
    private static MainActivity instance;
    private AlertDialog.Builder subDialog;

    String SetDate; // 선택 날짜 설정

    SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-M-d", Locale.KOREA);

    private TextView monthText;
    private GridView monthView;
    //사용한 금액(데이터베이스?)
    private ListView dailyAmountView;
    private MonthAdapter adapter1;
    /* private DailyAdapter adapter2;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "MainActivity.OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionRequester.Builder requester = new PermissionRequester.Builder(this);
        requester.create().request( Manifest.permission.RECEIVE_SMS, 10000, new PermissionRequester.OnClickDenyButtonListener()
        { @Override public void onClick(Activity activity) {
            Toast.makeText(MainActivity.this, "권한을 얻지 못했습니다.", Toast.LENGTH_SHORT).show(); } } );

        myRealm = Realm.getInstance(MainActivity.this);
        lvPersonNameList = (ListView) findViewById(R.id.lvPersonNameList);
        dataDetailsAdapter = new DataDetailsAdapter(MainActivity.this, dataDetailsModelArrayList);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);


        getAllWidgets();
        instance = this;
        setPersonDetailsAdapter();

        getAllUsers();//0528
        bindWidgetsWithEvents();

        ImageView addbtn=(ImageView) findViewById(R.id.addBtn);
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),DailyMoneySet.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
                finish();
            }
        });

        ImageView viewList=(ImageView) findViewById(R.id.viewList);
        viewList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(getApplicationContext(),DataList.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //getApplicationContext().startActivity(intent);
            }
        });


//추가
        fab1 = (FloatingActionButton)findViewById(R.id.fab_1);
        fab2 = (FloatingActionButton)findViewById(R.id.fab_2);
        fab4 = (FloatingActionButton)findViewById(R.id.fab_4);
        fab3 = (FloatingActionButton)findViewById(R.id.fab_3);

        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToggleFab();
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //새로추가를 눌렀을 때 null을 줌
                addOrUpdatePersonDetailsDialog(null,-1);
            }
        });
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), UpdateSpend.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
                finish();
            }
        });


        fab4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(getApplicationContext(), dataDetailsModelArrayList.get(0).getDate(), Toast.LENGTH_LONG).show();
            }
        });

        ProgressBar progressBar=(ProgressBar) findViewById(R.id.progressBar);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        //progressBar.setProgress(80);
        progressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "___만큼 달성하였습니다", Toast.LENGTH_LONG).show();
            }
        });

        monthText = (TextView) findViewById(R.id.monthText);
        monthView = (GridView) findViewById(R.id.calendarView);
      /*  dailyAmountView = (ListView) findViewById(R.id.listView);*/

        // 달력의 데이터
        adapter1 = new MonthAdapter(this);

        /*adapter2 = new DailyAdapter(this);

        adapter2.addAdapter(new accountItem("점심", 7000, R.drawable.stamp));
        adapter2.addAdapter(new accountItem("카페", 5900, R.drawable.stamp));*/

        monthView.setAdapter(adapter1);
      /*  dailyAmountView.setAdapter(adapter2);*/
        monthText.setText(adapter1.getCurrentYear() + "년" + adapter1.getCurrentMonth() + "월");

        monthView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showCostom(MainActivity.this,position);
            }
        });

        Button monthPrevious = (Button) findViewById(R.id.monthPrevious);
        // monthPrevious버튼 클릭시
        monthPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter1.setPriviousMonth();
                adapter1.notifyDataSetChanged();
                monthText.setText(adapter1.getCurrentYear() + "년" + adapter1.getCurrentMonth() + "월");
            }
        });
        // monthNext버튼 클릭시
        Button monthNext = (Button) findViewById(R.id.monthNext);
        monthNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter1.setNextMonth();
                adapter1.notifyDataSetChanged();
                monthText.setText(adapter1.getCurrentYear() + "년" + adapter1.getCurrentMonth() + "월");
            }
        });

    }
    public void showCostom(Context context, int position){

        Dialog dialog=new Dialog(context);
        // dialog화면의 정보를 lay_customdialog으로
        dialog.setContentView(R.layout.lay_customdialog);
        int result=0;

        TextView txt_year =(TextView)dialog.findViewById(R.id.txt_year);
        TextView txt_month=(TextView)dialog.findViewById(R.id.txt_month);
        TextView txt_day=(TextView)dialog.findViewById(R.id.txt_day);
        TextView dailyMoney=(TextView)dialog.findViewById(R.id.dailyMoney);
        TextView restMoney=(TextView)dialog.findViewById(R.id.restMoney);

        String year=String.valueOf(adapter1.getCurrentYear());
        String month=String.valueOf(adapter1.getCurrentMonth());
        String day=String.valueOf(adapter1.items[position].date);

        String dmoney="";
        String rmoney="";

        txt_year.setText(year);
        txt_month.setText(month);
        txt_day.setText(day);
        String datee=year+"-"+month+"-"+ day;

        dataDetailsAdapter.setDate(datee);

        //날짜별로 일일설정액 불러오기
        int number=0;
        for(int i=0; i<dataDetailsModelArrayList.size();i++) {

            while(dataDetailsModelArrayList.get(i).getstartDate()!=null) {
                if (datee.equals(dataDetailsModelArrayList.get(i).getstartDate().toString())){
                    number = i;

                    dmoney = String.valueOf(dataDetailsModelArrayList.get(number).getMoney_set());}
                break;
            }
        }
        dailyMoney.setText(dmoney);
        int number2=0;
        //남은금액 계산
        for(int i=0;i<dataDetailsModelArrayList.size();i++){
            while(dataDetailsModelArrayList.get(i).getPrice()!=0) {
                while(dataDetailsModelArrayList.get(i).isInOrOut()==true) {
                    if (datee.equals(dataDetailsModelArrayList.get(i).getDate().toString()))
                    {
                        number2 = i;
                        result += dataDetailsModelArrayList.get(number2).getPrice();
                    }
                    break;
                }
                break;
            }
        }
        if(dmoney!="") {
            rmoney = String.valueOf(Integer.valueOf(dmoney) - result);
            restMoney.setText(rmoney);
            if (Integer.valueOf(rmoney) < 0) {
                NotificationSomethings();
            }
        }else{
        }
        dialog.show();


    }
    //일일설정액db에서 데이터 가져오기
    private void getDailyMoney(){
        try {
            Date d = new SimpleDateFormat("yyyy-M-d").parse(SetDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //RealmResults<DailyMoneyModel> results = myRealm.where(DailyMoneyModel.class).findAll();
        //Log.e("ee", results.get(0).getEndDate());

    }
    //푸시알림 설정
    public void NotificationSomethings(){
        Resources res=getResources();
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this);
        builder.setContentTitle("일일설정액 초과!")
                .setContentText("그만 써!")
                .setTicker("일일 설정액 초과!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res,R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL);
        if(android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP){
            builder.setCategory(Notification.CATEGORY_MESSAGE)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        NotificationManager nm=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(20,builder.build());
    }

    public static MainActivity getInstance() {
        Log.e(LOG_TAG, "DataList.getInstance");
        return instance;
    }

    private void setPersonDetailsAdapter() {
        Log.e(LOG_TAG, "DataList.setPersonDetailsAdapter");
        dataDetailsAdapter = new DataDetailsAdapter(MainActivity.this, dataDetailsModelArrayList);
        dataDetailsAdapter.setDate(transFormat.format(new Date()));

        lvPersonNameList.setAdapter(dataDetailsAdapter);//데이터 리스트 보여주는 함수
    }

    public void deleteData(int personId, int position) {
        Log.e(LOG_TAG, "DataList.deletePerson");
        RealmResults<DataDetailsModel> results = myRealm.where(DataDetailsModel.class).equalTo("id", personId).findAll();

        myRealm.beginTransaction();
        results.remove(0);
        myRealm.commitTransaction();
        dataDetailsModelArrayList.remove(position);
        dataDetailsAdapter.notifyDataSetChanged();
    }
    public DataDetailsModel searchData(int personId) {
        Log.e(LOG_TAG, "DataList.searchPerson");
        RealmResults<DataDetailsModel> results = myRealm.where(DataDetailsModel.class).equalTo("id", personId).findAll();
        myRealm.beginTransaction();
        myRealm.commitTransaction();
        return results.get(0);
    }

    private void ToggleFab() {
        // 버튼들이 보여지고있는 상태인 경우 숨겨줍니다.
        if(fab1.getVisibility() == View.VISIBLE) {
            fab1.hide();
            fab2.hide();
            fab4.hide();
            fab1.animate().translationY(0);
            fab2.animate().translationY(0);
            fab4.animate().translationY(0);
        }
        // 버튼들이 숨겨져있는 상태인 경우 위로 올라오면서 보여줍니다.
        else {
            // 중심이 되는 버튼의 높이 + 마진 만큼 거리를 계산합니다.
            int dy = fab3.getHeight() + 20;
            fab1.show();
            fab2.show();
            fab4.show();
            // 계산된 거리만큼 이동하는 애니메이션을 입력합니다.
            fab4.animate().translationY(-dy*3);
            fab1.animate().translationY(-dy*2);
            fab2.animate().translationY(-dy);
        }
    }
    //동그라미버튼
    private void getAllWidgets() {
        Log.e(LOG_TAG, "MainActivity.getAllWidgets");
        //fabAddPerson = (FloatingActionButton) findViewById(R.id.fab);
        fab2 = (FloatingActionButton)findViewById(R.id.fab_2);
        lvPersonNameList = (ListView) findViewById(R.id.lvPersonNameList);
    }
    private void bindWidgetsWithEvents() {
        Log.e(LOG_TAG, "MainActivity.bindWidgetsWithEvents");
        //fabAddPerson.setOnClickListener(this);
        fab2.setOnClickListener(this);

    }

    //수정
    @Override
    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.fab_2:
//                addOrUpdatePersonDetailsDialog(null,-1);
//                break;
//            case R.id.fab_1:
//                Toast.makeText(MainActivity.this, "영수증인식", Toast.LENGTH_SHORT).show();
//                //Intent intent = new Intent(getApplicationContext(), UpdateSpend.class);
//                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                //getApplicationContext().startActivity(intent);
//                break;
//
        //       }
    }



    //다이얼로그를 열어 데이터를 추가 + 삭제 하는 함수
    public void addOrUpdatePersonDetailsDialog(final DataDetailsModel model,final int position) {
//subdialog
        Log.e(LOG_TAG, "MainActivity.addOrUpdatePersonDetailsDialog");
        subDialog = new AlertDialog.Builder(MainActivity.this)//입력이 다 되지 아니하였을 때 나타나는 다이얼로그
                .setMessage("모두 입력해주세요")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg2, int which) {
                        dlg2.cancel();
                    }
                });
//maindialog
        LayoutInflater li = LayoutInflater.from(MainActivity.this);//뷰를 띄워주는 역할
        View promptsView = li.inflate(R.layout.mainincome_dialog, null);//정보를 입력하는 창 연결, 뷰 생성
        AlertDialog.Builder mainDialog = new AlertDialog.Builder(MainActivity.this);//다이얼 로그 생성하기 위한 빌더 얻기
        mainDialog.setView(promptsView);//알림창 지정된 레이아웃을 띄운다
        mainDialog.setTitle("수입 입력");

        //이 변수들은 income_dialog.xml에서 가져온 아이들, 즉 한 엑티비티에 뷰를 두개 가져온 것이다
        //위에서 View promptsViewView이 문장을 통해 뷰를 생성했기 때문에 사용이 가능하다
        final Spinner etAddCategory = (Spinner) promptsView.findViewById(R.id.setCategory);
        final EditText etAddIncome = (EditText) promptsView.findViewById(R.id.setIncome);

        adapter = ArrayAdapter.createFromResource(this,
                R.array.UISpinner,//배열 가져온다
                android.R.layout.simple_spinner_item);//어떤형식으로
        etAddCategory.setAdapter(adapter);



        //모델이 없다면, 즉 새로운 데이터를 입력한다면
        //버튼을 눌렀을 때 이 함수에 null,-l을 매개변수로 주는것을 볼 수 있다. null을 준 의미가 새로운 데이터를 생성하기 위함임
        //뷰를 띄우고 기다림
        if (model != null) {
            etAddCategory.setAdapter(adapter);//스피너와 연결!!
            etAddIncome.setText(String.valueOf(model.getPrice()));
        }
        mainDialog.setCancelable(false)//back키 설정 안함
                .setPositiveButton("Ok", null)//ok버튼 설정
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {// cancel버튼 설정
                        dialog.cancel();
                    }
                });

        final AlertDialog dialog = mainDialog.create();//다이얼 로그 객체 얻어오기
        dialog.show();// 다이얼로그 보여주기
        Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);//ok버튼 누르게 된다면
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //입력칸이 비어있는지 확인하고 다 채워졌다면 데이터를 추가 or업데이트, 빈 칸이 있다면 채우라는 다이얼로그띄움
                // if (!Utility.isBlankField(etAddCategory) && !Utility.isBlankField(etAddIncome)) {
                if (!Utility.isBlankField(etAddIncome)) {
                    String selItem = (String)etAddCategory.getSelectedItem();
                    DataDetailsModel dataDetailsModel = new DataDetailsModel();
                    dataDetailsModel.setName(selItem);
                    dataDetailsModel.setPrice(Integer.parseInt(etAddIncome.getText().toString()));
                    dataDetailsModel.setDate(transFormat.format(new Date())); //date추가

                    Log.d("ee",dataDetailsModel.getDate().toString());
                    if (model == null)//데이터베이스를 새로 생성하겠다!!
                        addDataToRealm(dataDetailsModel);
                    else//기존에 있던 데이터를 업데이트하겠다!!
                        updatePersonDetails(dataDetailsModel, position, model.getId());
                    dialog.cancel();
                } else {//다이얼 로그가 비워져 있다면 이것을 보여줘!!
                    subDialog.show();
                }
            }
        });
    }
    //데이터 삽입함수
    private void addDataToRealm(DataDetailsModel model) {
        Log.e(LOG_TAG, "DataList.addDataToRealm");


        myRealm.beginTransaction();

        DataDetailsModel dataDetailsModel = myRealm.createObject(DataDetailsModel.class);

        dataDetailsModel.setId(id+dataDetailsModelArrayList.size()+1); //id+남아있는리스트개수를 해줘야해

        dataDetailsModel.setName(model.getName());

        dataDetailsModel.setPrice(model.getPrice());
        dataDetailsModel.setDate(model.getDate());
        dataDetailsModel.setInOrOut(false); //수입
        dataDetailsModelArrayList.add(dataDetailsModel);
        myRealm.commitTransaction();
        dataDetailsAdapter.notifyDataSetChanged();
        id++;
    }

    //데이터 업데이트 함(수정)
    public void updatePersonDetails(DataDetailsModel model,int position,int personID) {
        Log.e(LOG_TAG, "MainActivity.updatePersonDetails");
        DataDetailsModel editPersonDetails = myRealm.where(DataDetailsModel.class).equalTo("id", personID).findFirst();
        myRealm.beginTransaction();
        editPersonDetails.setName(model.getName());
        editPersonDetails.setPrice(model.getPrice());
        myRealm.commitTransaction();
        dataDetailsModelArrayList.set(position, editPersonDetails);
        dataDetailsAdapter.notifyDataSetChanged();
    }



    //0528
    //데이터 리스트 가져오는 함수
    private void getAllUsers() {
        Log.e(LOG_TAG, "DataList.getAllUsers");
        RealmResults<DataDetailsModel> results = myRealm.where(DataDetailsModel.class).findAll();
        myRealm.beginTransaction();

        for (int i = 0; i < results.size(); i++) {
            dataDetailsModelArrayList.add(results.get(i));
        }
        if(results.size()>0)
            id = myRealm.where(DataDetailsModel.class).max("id").intValue() + 1;
        myRealm.commitTransaction();
        dataDetailsAdapter.notifyDataSetChanged();

    }

    public void addOrUpdatePersonDetailsDialog22(final DataDetailsModel model,final int position) {
        //subdialog
        Log.e(LOG_TAG, "DataList.addOrUpdatePersonDetailsDialog");
        subDialog = new AlertDialog.Builder(MainActivity.this)
                .setMessage("모두 입력해주세요")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg2, int which) {
                        dlg2.cancel();
                    }
                });
        //maindialog
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View promptsView = li.inflate(R.layout.income_dialog, null);
        AlertDialog.Builder mainDialog = new AlertDialog.Builder(MainActivity.this);
        mainDialog.setView(promptsView);
        final EditText etAddPersonName = (EditText) promptsView.findViewById(R.id.setCategory);
        final EditText etAddPersonAge = (EditText) promptsView.findViewById(R.id.setIncome);
        if (model != null) {
            etAddPersonName.setText(model.getName());
            etAddPersonAge.setText(String.valueOf(model.getPrice()));
        }
        mainDialog.setCancelable(false)
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        final AlertDialog dialog = mainDialog.create();
        dialog.show();
        Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Utility.isBlankField(etAddPersonName) && !Utility.isBlankField(etAddPersonAge)) {
                    DataDetailsModel dataDetailsModel = new DataDetailsModel();
                    dataDetailsModel.setName(etAddPersonName.getText().toString());
                    dataDetailsModel.setPrice(Integer.parseInt(etAddPersonAge.getText().toString()));
                    //dataDetailsModel.setDate(new Date());
                    dataDetailsModel.setMoney_set(model.getMoney_set());
                    if (model == null)
                        Log.d("ee","nono");
                        // addDataToRealm(dataDetailsModel);
                    else
                        updatePersonDetails(dataDetailsModel, position, model.getId());
                    dialog.cancel();
                } else {
                    subDialog.show();
                }
            }
        });
    }
    private void addDataToRealm22(DataDetailsModel model) {
        Log.e(LOG_TAG, "DataList.addDataToRealm");


        myRealm.beginTransaction();

        DataDetailsModel dataDetailsModel = myRealm.createObject(DataDetailsModel.class);
        dataDetailsModel.setId(id+dataDetailsModelArrayList.size()); //id+남아있는리스트개수를 해줘야해
        dataDetailsModel.setName(model.getName());
        dataDetailsModel.setPrice(model.getPrice());
        dataDetailsModel.setDate(model.getDate());
        dataDetailsModel.setMoney_set(model.getMoney_set());
        dataDetailsModelArrayList.add(dataDetailsModel);
        myRealm.commitTransaction();
        dataDetailsAdapter.notifyDataSetChanged();
        id++;
    }

    public void updatePersonDetails22(DataDetailsModel model,int position,int personID) {
        Log.e(LOG_TAG, "DataList.updatePersonDetails");
        DataDetailsModel editPersonDetails = myRealm.where(DataDetailsModel.class).equalTo("id", personID).findFirst();
        myRealm.beginTransaction();
        editPersonDetails.setName(model.getName());
        editPersonDetails.setPrice(model.getPrice());
        myRealm.commitTransaction();
        dataDetailsModelArrayList.set(position, editPersonDetails);
        dataDetailsAdapter.notifyDataSetChanged();
    }

    // db삭제
    // 앱이 종료되었을  onCreate와 반대로 액티비티가 종료 될 때 onDestroy가 나타난다
    protected void onDestroy() {
        Log.e(LOG_TAG, "MainActivity.onDestroy");
        super.onDestroy();
        dataDetailsModelArrayList.clear();
        myRealm.close();
    }









}