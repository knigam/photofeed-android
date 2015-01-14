package com.keonasoft.photofeed.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.keonasoft.photofeed.app.helper.HttpHelper;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;
    private FloatingActionButton cameraBtn;
    private ImageView mImageView;
    private ArrayList<String> drawerListItems;
    private Map<Integer, String> albumUrls;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private ArrayList<HashMap<String,String>> pictureListItems;
    private ListView picturesList;
    private Map<Integer, String> pictureUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HttpHelper.getInstance().initialize(getApplicationContext());

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        picturesList = (ListView) findViewById(R.id.pictures_list);

        updateDrawerListItems();
        populatePicturesList();

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                HttpHelper.getInstance().getClient().get(albumUrls.get(position), new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody,0, responseBody.length);
                        mImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });
                mDrawerList.setItemChecked(position, true);
                setTitle(drawerListItems.get(position));
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });

        cameraBtn = (FloatingActionButton) findViewById(R.id.camera_button);
        //TODO: MOVE NEXT LINE TO NEW ACTIVITY
        // mImageView = (ImageView) findViewById(R.id.thumbnail);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });


    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            //Todo: send picture to server
            final String URL = getString(R.string.conn) + getString(R.string.picture_create);
            final File image = new File(mCurrentPhotoPath);
            RequestParams params = new RequestParams();
            params.put("text", image.getName());
            try {
                params.put("image", image);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            final String[] IMAGEURL = new String[1];
            HttpHelper.getInstance().getClient().post(URL, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    JSONObject json;
                    final String CONN = getString(R.string.conn);
                    String imageUrl = CONN + "/404.html";
                    try {
                        json = new JSONObject(new String(responseBody));
                        imageUrl = CONN + json.getString("image_url");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    HttpHelper.getInstance().getClient().get(imageUrl, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody,0, responseBody.length);
                            mImageView.setImageBitmap(bitmap);
                            setTitle(image.getName());
                            updateDrawerListItems();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                        }
                    });
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(MainActivity.this, "Sending failed", Toast.LENGTH_SHORT);
                }
            });


        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File to save the photo
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException e) {
                // Error while creating the file
                System.out.println("ERROR CREATING THE FILE");
                return;
            }
            // Continue if there was no error
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void updateDrawerListItems() {
        albumUrls = new HashMap<Integer, String>();
        drawerListItems = new ArrayList<String>();
        HttpHelper.getInstance().getClient().get(getString(R.string.conn) + getString(R.string.picture_index), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONArray json;
                try {
                    json = new JSONArray(new String(responseBody));
                    for (int i = 0; i < json.length(); i++){
                        JSONObject j = json.getJSONObject(i);
                        drawerListItems.add(j.getString("text"));
                        albumUrls.put(i, getString(R.string.conn) + j.getString("original_url"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mDrawerList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, drawerListItems));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    private void populatePicturesList() {
        pictureListItems = new ArrayList<HashMap<String, String>>();

        HttpHelper.getInstance().getClient().get(getString(R.string.conn) + getString(R.string.picture_index), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONArray json;
                try {
                    json = new JSONArray(new String(responseBody));
                    for (int i = 0; i < json.length(); i++){
                        JSONObject j = json.getJSONObject(i);
                        final HashMap<String,String> item = new HashMap<String, String>();
                        item.put("picture_text", j.getString("text"));
                        item.put("picture_thumb", j.getString("thumb_url"));
                        item.put("picture_orig", j.getString("original_url"));

                        pictureListItems.add(item);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
        PictureAdapter adapter = new PictureAdapter(getBaseContext(), pictureListItems);
        picturesList.setAdapter(adapter);
    }

    private class PictureAdapter extends BaseAdapter {
        List<HashMap<String, String>> data;
        LayoutInflater inflater;

        public PictureAdapter(Context context, List<HashMap<String, String>> data){
            this.data = data;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public HashMap<String, String> getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class Holder{
            TextView tv;
            ImageView img;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final Holder holder = new Holder();
            View rowView;
            rowView = inflater.inflate(R.layout.picture_item, null);
            holder.tv=(TextView) rowView.findViewById(R.id.picture_text);
            holder.img=(ImageView) rowView.findViewById(R.id.picture_thumb);

            holder.tv.setText(data.get(position).get("picture_text"));

            HttpHelper.getInstance().getClient().get(getString(R.string.conn) + data.get(position).get("picture_thumb"), new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody,0, responseBody.length);
                    holder.img.setImageBitmap(bitmap);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                }
            });

//            rowView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Toast.makeText(context, "You Clicked "+result[position], Toast.LENGTH_LONG).show();
//                }
//            });
            return rowView;
        }
    }
}
