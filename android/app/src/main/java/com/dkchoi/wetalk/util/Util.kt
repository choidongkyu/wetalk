package com.dkchoi.wetalk.util

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.dkchoi.wetalk.data.PhoneBook

class Util {
    companion object {
        fun setSession(id: String, context: Context) {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(
                    Config.SESSION,
                    Context.MODE_PRIVATE
                ) //session에 관련된 pref를 얻어옴

            val editor = sharedPreferences.edit()
            editor.putString(Config.SESSION_KEY, id) //id을 sharedPref에 저장
            editor.apply()
        }

        fun getSession(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(Config.SESSION, Context.MODE_PRIVATE)
            return sharedPreferences.getString(Config.SESSION_KEY, null)
        }

        fun getContacts(context: Context): MutableList<PhoneBook> {
            // 데이터베이스 혹은 content resolver 를 통해 가져온 데이터를 적재할 저장소를 먼저 정의
            val datas: MutableList<PhoneBook> = mutableListOf()

            // 1. Resolver 가져오기(데이터베이스 열어주기)
            // 전화번호부에 이미 만들어져 있는 ContentProvider 를 통해 데이터를 가져올 수 있음
            // 다른 앱에 데이터를 제공할 수 있도록 하고 싶으면 ContentProvider 를 설정
            // 핸드폰 기본 앱 들 중 데이터가 존재하는 앱들은 Content Provider 를 갖는다
            // ContentResolver 는 ContentProvider 를 가져오는 통신 수단
            val resolver: ContentResolver = context.contentResolver

            // 2. 전화번호가 저장되어 있는 테이블 주소값(Uri)을 가져오기
            val phoneUri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            // 3. 테이블에 정의된 칼럼 가져오기
            // ContactsContract.CommonDataKinds.Phone 이 경로에 상수로 칼럼이 정의
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID, // 인덱스 값, 중복될 수 있음 -- 한 사람 번호가 여러개인 경우
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            // 4. ContentResolver로 쿼리를 날림 -> resolver 가 provider 에게 쿼리하겠다고 요청
            val cursor: Cursor? = resolver.query(phoneUri, projection, null, null, null)

            // 4. 커서로 리턴된다. 반복문을 돌면서 cursor 에 담긴 데이터를 하나씩 추출
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // 4.1 이름으로 인덱스를 찾아준다
                    val idIndex: Int = cursor.getColumnIndex(projection[0]) // 이름을 넣어주면 그 칼럼을 가져와준다.
                    val nameIndex: Int = cursor.getColumnIndex(projection[1])
                    val numberIndex: Int = cursor.getColumnIndex(projection[2])
                    // 4.2 해당 index 를 사용해서 실제 값을 가져온다.
                    val id: String = cursor.getString(idIndex)
                    val name: String = cursor.getString(nameIndex)
                    val number: String = cursor.getString(numberIndex)
                    val phoneBook = PhoneBook()
                    phoneBook.id = id
                    phoneBook.name = name
                    phoneBook.tel = number
                    datas.add(phoneBook)
                }
            }
            // 데이터 계열은 반드시 닫아줘야 한다.
            cursor?.close()
            return datas
        }
    }
}