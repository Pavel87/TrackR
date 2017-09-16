package com.pacmac.trackr;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;


/**
 * Created by pacmac on 2017-08-14.
 */


public final class ImageGallery extends AppCompatActivity {

    private GridView galleryView;
    private GalleryAdapter galleryAdapter;
    private ArrayList<ImageItem> imageDataSet = new ArrayList<>();
    int origImg = 0;
    int resultImg = -1;
    View view = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarGallery);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        origImg = getIntent().getIntExtra(Constants.EDIT_USER_IMG, 0);

        imageDataSet = getData();
        galleryView = (GridView) findViewById(R.id.gridView);
        galleryAdapter = new GalleryAdapter(this, R.layout.gallery_item, imageDataSet, origImg);
        galleryView.setAdapter(galleryAdapter);

        galleryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                resultImg = imageDataSet.get(i).getId();
                galleryAdapter.selectView(resultImg);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (resultImg == 0 || resultImg == origImg) {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
            return;
        }
        Intent intent = getIntent();
        intent.putExtra(Constants.EDIT_USER_IMG, resultImg);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
        return;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (resultImg == -1 || resultImg == origImg) {
                    setResult(RESULT_CANCELED);
                    onBackPressed();
                    return true;
                }
                Intent intent = getIntent();
                intent.putExtra(Constants.EDIT_USER_IMG, resultImg);
                setResult(RESULT_OK, intent);
                onBackPressed();
                break;
            default:
                break;
        }
        return true;
    }

    // load local images for user records
    private ArrayList<ImageItem> getData() {
        final ArrayList<ImageItem> imageItems = new ArrayList<>();
        TypedArray stockImages = getResources().obtainTypedArray(R.array.stockImages);
        for (int i = 0; i < stockImages.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), stockImages.getResourceId(i, 0));
            imageItems.add(new ImageItem(bitmap, i));
        }
        return imageItems;
    }




}
