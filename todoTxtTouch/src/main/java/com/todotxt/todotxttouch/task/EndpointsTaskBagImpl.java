package com.todotxt.todotxttouch.task;

import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.todotxt.backend.taskApi.TaskApi;
import com.google.todotxt.backend.taskApi.model.TaskBean;
import com.todotxt.todotxttouch.TodoPreferences;
import com.todotxt.todotxttouch.util.TaskIo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by aawans on 6/14/14.
 */
public class EndpointsTaskBagImpl extends TaskBagImpl {
    final TaskApi taskApiService;

    public EndpointsTaskBagImpl(TodoPreferences preferences, LocalTaskRepository localRepository) {
        super(preferences, localRepository, null);
        TaskApi.Builder builder = new TaskApi.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null);
        taskApiService = builder.build();
    }

    @Override
    public synchronized void pushToRemote (boolean overridePreference, boolean overwrite){
        try {
            ArrayList<String> taskStrList =
                    TaskIo.loadTasksStrFromFile(LocalFileTaskRepository.TODO_TXT_FILE);
            taskApiService.clearTasks().execute();

            long id = 1;
            for (String taskStr : taskStrList){
                TaskBean taskBean = new TaskBean();
                taskBean.setData(taskStr);
                taskBean.setId(id++);
                taskApiService.storeTask(taskBean).execute();
            }

            lastSync = new Date();
        } catch (IOException e){
            Log.e(EndpointsTaskBagImpl.class.getSimpleName(),
            "Error when storing tasks", e);
        }
    }

    @Override
    public synchronized void pullFromRemote(boolean overridePreference) {
        try {
            // Remote call
            List<TaskBean> remoteTasks = taskApiService.getTasks().execute().getItems();
            if (remoteTasks != null){
                ArrayList<Task> taskList = new ArrayList<Task>();
                for(TaskBean taskBean : remoteTasks){
                    taskList.add(new Task(taskBean.getId(), taskBean.getData()));
                }
                store(taskList);
                reload();
                lastSync = new Date();
            }
        } catch (IOException e){
            Log.e(EndpointsTaskBagImpl.class.getSimpleName(), "Error when loading task", e);
        }
    }
}
