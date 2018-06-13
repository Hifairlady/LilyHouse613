package com.edgar.lilyhouse.Data.MainSceneData;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import com.edgar.lilyhouse.Network.JsonStringGetter;
import com.edgar.lilyhouse.Network.NetworkResponse;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MangaRepository {

    private MangaItemDao itemDao;

    private LiveData<List<MangaItem>> listItems;

    public MangaRepository(Context context) {

        MangaDatabase roomDatabase = MangaDatabase.getInstance(context);
        itemDao = roomDatabase.mainListItemDao();

    }

    public LiveData<List<MangaItem>> getListItems(int order) {
        if (order == 1) {
            return itemDao.getLatestUpdateItems();
        }
        return itemDao.getMostViewedItems();

    }

    public LiveData<MangaItem> getListItemById(int id) {
        return itemDao.getItemById(id);
    }

    public void deleteItem(MangaItem listItem) {
        new DeleteItemAsync(itemDao).execute(listItem);
    }

    public void deleteAllItems() {
        new DeleteAllItemsAsync(itemDao).execute();
    }

    public void insertItems(MangaItem... mangaItems) {
        new InsertAllItemsAsync(itemDao).execute(mangaItems);
    }

    public void refreshData(String queryUrl, NetworkResponse response) {
        new RefreshDataAsync(itemDao, queryUrl, response).execute();
    }

    //database access async tasks
    protected static class DeleteItemAsync extends AsyncTask<MangaItem, Void, Void> {

        private MangaItemDao itemDao;

        protected DeleteItemAsync(MangaItemDao itemDao) {
            this.itemDao = itemDao;
        }

        @Override
        protected Void doInBackground(MangaItem... mangaItems) {
            itemDao.deleteItem(mangaItems[0]);
            return null;
        }
    }

    protected static class DeleteAllItemsAsync extends AsyncTask<Void, Void, Void> {

        private MangaItemDao itemDao;

        protected DeleteAllItemsAsync(MangaItemDao itemDao) {
            this.itemDao = itemDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            itemDao.deleteAllItems();
            return null;
        }
    }

    protected static class InsertAllItemsAsync extends AsyncTask<MangaItem, Void, Void> {

        private MangaItemDao itemDao;

        protected InsertAllItemsAsync(MangaItemDao itemDao) {
            this.itemDao = itemDao;
        }

        @Override
        protected Void doInBackground(MangaItem... mangaItems) {
            itemDao.insertItems(mangaItems);
            return null;
        }
    }

    protected static class RefreshDataAsync extends AsyncTask<Void, Void, Integer> {

        private static final String TAG = "==============" + RefreshDataAsync.class.getSimpleName();
        private MangaItemDao itemDao;
        private String queryUrl;
        private int resultCode;
        private NetworkResponse response;

        public RefreshDataAsync(MangaItemDao itemDao, String queryUrl, NetworkResponse response) {
            this.itemDao = itemDao;
            this.queryUrl = queryUrl;
            this.resultCode = 101;
            this.response = response;
        }

        @Override
        protected Integer doInBackground(Void... voids) {

            //fetch data from network and insert into database

            new JsonStringGetter() {
                @Override
                public void onLoadFailed(int code) {
                    resultCode = code;
                }

                @Override
                public void onLoadSucceed(String jsonString) {

                    Type listType = new TypeToken<ArrayList<MangaItem>>() {}.getType();
                    ArrayList<MangaItem> items = new GsonBuilder().create().fromJson(jsonString, listType);
                    itemDao.insertItemsInList(items);
                    resultCode = 100;

                }
            }.loadJsonString(queryUrl);

            return resultCode;
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            super.onPostExecute(resultCode);

            if (resultCode == 100) {
                response.onSucceed();
            } else {
                response.onFailed(resultCode);
            }

        }
    }

}
