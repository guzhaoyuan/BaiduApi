package baidumapsdk.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class Intelligent_electrombile extends Activity {

    private EditText username;
    private EditText password;
    private Button login;
    private CheckBox cb;
    private TextView loginLockedTV;
    private TextView attemptsLeftTV;
    private TextView numberOfRemainingLoginAttemptsTV;
    int numberOfRemainingLoginAttempts = 3;
    Map<String,String> map;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intelligent_electrombile);
        setupVariables();
        map = LoginService.getSavedUserInfo(this);
        if(map!=null){
            username.setText(map.get(username));
            password.setText(map.get(password));
        }
    }


    public void authenticateLogin(View view) {
        if(cb.isChecked()){
            boolean result = LoginService.saveUserInfo(this,username.getText().toString(),password.getText().toString());
            if(result)
                Toast.makeText(this, "Save Successfully", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this,"Save Unsuccessfully",Toast.LENGTH_SHORT).show();
        }
        if (username.getText().toString().equals("gzy") &&//发送用户名到服务器
                password.getText().toString().equals("guzhaoyuan")) {
            Intent intent =new Intent();
            intent.setClass(Intelligent_electrombile.this,info.class);
            startActivity(intent);
            Intelligent_electrombile.this.finish();
        } else {
            Toast.makeText(getApplicationContext(), "username and password do not match",
                    Toast.LENGTH_SHORT).show();
            numberOfRemainingLoginAttempts--;
            attemptsLeftTV.setVisibility(View.VISIBLE);
            numberOfRemainingLoginAttemptsTV.setVisibility(View.VISIBLE);
            numberOfRemainingLoginAttemptsTV.setText(Integer.toString(numberOfRemainingLoginAttempts));

            if (numberOfRemainingLoginAttempts == 0) {
                login.setEnabled(false);
                loginLockedTV.setVisibility(View.VISIBLE);
                loginLockedTV.setBackgroundColor(Color.RED);
                loginLockedTV.setText("LOGIN LOCKED!!!");
            }
        }
    }

    private void setupVariables() {
        username = (EditText) findViewById(R.id.usernameET);
        password = (EditText) findViewById(R.id.passwordET);
        login = (Button) findViewById(R.id.loginBtn);
        loginLockedTV = (TextView) findViewById(R.id.loginLockedTV);
        attemptsLeftTV = (TextView) findViewById(R.id.attemptsLeftTV);
        numberOfRemainingLoginAttemptsTV = (TextView) findViewById(R.id.numberOfRemainingLoginAttemptsTV);
        numberOfRemainingLoginAttemptsTV.setText(Integer.toString(numberOfRemainingLoginAttempts));
        cb = (CheckBox)findViewById(R.id.checkbox);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }
}
