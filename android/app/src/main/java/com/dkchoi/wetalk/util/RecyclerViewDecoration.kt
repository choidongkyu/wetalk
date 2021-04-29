package com.dkchoi.wetalk.util

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class RecyclerViewDecoration(  //리사이클러뷰 가로 간격 조절하기 위한 클래스
    private val divWidth: Int
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.right = divWidth
        outRect.left = divWidth
        outRect.bottom = divWidth
    }
}