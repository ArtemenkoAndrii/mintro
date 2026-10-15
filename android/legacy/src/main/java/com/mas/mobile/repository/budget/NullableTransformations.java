package com.mas.mobile.repository.budget;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mas.mobile.repository.db.entity.Budget;

import java.util.function.Function;


public class NullableTransformations {
    /*
        Despite T isn't nullable in Kotlin the LiveData<T> emits null if the record is deleted
     */
    public static <I, O> LiveData<O> map(LiveData<I> live, Function<I, O> hook) {
        return Transformations.map(live, hook::apply);
    }
}
