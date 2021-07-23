package com.dkchoi.wetalk.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dkchoi.wetalk.data.ChatRoom
import com.dkchoi.wetalk.data.MessageData
import com.dkchoi.wetalk.data.PhoneBook
import com.dkchoi.wetalk.data.User
import com.dkchoi.wetalk.retrofit.BackendInterface
import com.dkchoi.wetalk.retrofit.ServiceGenerator
import com.dkchoi.wetalk.room.AppDatabase
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class Util {
    companion object {
        private val server = ServiceGenerator.retrofitUser.create(BackendInterface::class.java)

        val gson = Gson()

        const val profileImgPath = "http://49.247.19.12/profile_image"
        const val chatImgPath = "http://49.247.19.12/chatImage"
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

        fun setMyStatueMsg(msg: String, context: Context) {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(
                    Config.MY_PROFILE,
                    Context.MODE_PRIVATE
                ) //session에 관련된 pref를 얻어옴

            val editor = sharedPreferences.edit()
            editor.putString(Config.MY_STATUS, msg) //상태메시지를 sharedPref에 저장
            editor.apply()
        }

        fun getMyUser(context: Context): User {
            return User(
                getPhoneNumber(context),
                getMyImg(context),
                getMyStatusMsg(context),
                getMyName(context)!!
            )
        }

        fun setMyImg(msg: String, context: Context) {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(
                    Config.MY_PROFILE,
                    Context.MODE_PRIVATE
                ) //session에 관련된 pref를 얻어옴

            val editor = sharedPreferences.edit()
            editor.putString(Config.MY_IMG, msg) //이미지 키를 sharedPref에 저장
            editor.apply()
        }

        fun getMyImg(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(Config.MY_PROFILE, Context.MODE_PRIVATE)
            return sharedPreferences.getString(Config.MY_IMG, null)
        }

        fun getMyStatusMsg(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(Config.MY_PROFILE, Context.MODE_PRIVATE)
            return sharedPreferences.getString(Config.MY_STATUS, null)
        }

        fun setMyName(msg: String, context: Context) {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(
                    Config.MY_PROFILE,
                    Context.MODE_PRIVATE
                ) //session에 관련된 pref를 얻어옴

            val editor = sharedPreferences.edit()
            editor.putString(Config.MY_NAME, msg) //상태메시지를 sharedPref에 저장
            editor.apply()
        }

        fun getMyName(context: Context): String? {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(Config.MY_PROFILE, Context.MODE_PRIVATE)
            return sharedPreferences.getString(Config.MY_NAME, null)
        }

        suspend fun fetchUserData(application: Application) {
            // 서버에서 받아온 유저 정보를 데이터베이스에 저장하기 위해 db객체 생성
            val db = AppDatabase.getInstance(application, "user-database")

            val phoneBooks: List<PhoneBook> = Util.getContacts(application) //전화번호부 가져옴

            //유저정보를 받기위한 retrofit 객체 생성
            val server = ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
            val friendList = mutableListOf<User>()
            val userList = server.getUserList()

            //서버에서 받아온 user 리스트와 전화번호부 비교하여 친구리스트 생성
            for (user in userList) {
                for (phoneBook in phoneBooks) {
                    var result = phoneBook.tel?.replace("-", "") // '-' 제거
                    result = result?.replaceFirst("0", "+82")
                    if (user.id == result) { //서버에 있는 유저가 전화번호부에 있다면
                        friendList.add(user) // 추가
                    }

                    //에뮬레이터 번호 예외처리, 테스트용으로 list에 추가
                    if (user.name == "에뮬레이터" && phoneBook.name == "에뮬레이터") {
                        friendList.add(user)
                    }
                }
            }
            if (friendList.size != 0) {
                friendList.sortBy { it.name }
                db?.userDao()?.insertAll(*friendList.toTypedArray())
            }
        }

        fun getUserIdsFromRoomName(roomName: String): List<String> { // roomName으로부터 user id를 파싱해주는 메소드
            return roomName.split(",") //room name에 포함된 userid 파싱
        }

        //내정보를 서버에서 갖고 오기 위한 메소드
        suspend fun fetchMyData(context: Context) {
            val server = ServiceGenerator.retrofitUser.create(BackendInterface::class.java)
            val user = server.getUser(getPhoneNumber(context))
            if (user.profileImage != null) {
                setMyImg(user.profileImage!!, context)
            } else {
                setMyImg("null", context)
            }

            if (user.profileText != null) {
                setMyStatueMsg(user.profileText!!, context)
            } else {
                setMyStatueMsg("", context)
            }
            setMyName(user.name, context)
        }


        // 해당 기능의 권한이 있는지 확인할 수 있는 메소드
        fun hasPermissions(permissions: Array<String>, context: Context): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                )
                    return false
            }
            return true
        }

        @SuppressLint("MissingPermission")
        fun getPhoneNumber(context: Context): String {
            val telManager: TelephonyManager =
                context.getSystemService(AppCompatActivity.TELEPHONY_SERVICE) as TelephonyManager
            var phoneNumber: String = telManager.line1Number
            //kt의 경우 국가번호 +82가 붙지만 그외에 통신사는 붙지 않음 때문에 국가번호를 붙여줘야함
            if (!phoneNumber.contains("+82")) {
                phoneNumber = phoneNumber.replaceFirst("0", "+82")
            }
            return phoneNumber
        }

        @SuppressLint("SimpleDateFormat")
        fun Long.toDate(): String {
            return SimpleDateFormat("hh:mm a").format(Date(this))
        }

        fun getRoomImagePath(roomName: String, context: Context): String {
            var userId = ""
            val users = roomName.split(",") //room name에 포함된 userid 파싱
            for (user in users) {
                if (user != Util.getPhoneNumber(context)) {//자신이 아닌 다른 user의 프로필 사진으로 채팅방 구성
                    userId = user
                    break
                }
            }
            val imgPath = "${Util.profileImgPath}/${userId}.jpg"
            return imgPath
        }


        //소켓으로 부터 들어온 메시지를 Room에 저장하는 메소드
        suspend fun saveMsgToLocalRoom(message: String, db: AppDatabase, context: Context) {
            val messageData: MessageData = gson.fromJson(message, MessageData::class.java)
            if (db.chatRoomDao()?.getRoomFromName(messageData.roomName) == null) { // 로컬 db에 존재하는 방이 없다면
                val ids: List<String> = getUserIdsFromRoomName(messageData.roomName)
                val userList = server.getUserListByIds(ids) // room에 소속된 user list 가져옴
                val imgPath = getRoomImagePath(messageData.roomName, context)
                val chatRoom =
                    ChatRoom(
                        "$message|", imgPath,
                        null, 1, userList.toMutableList()) //adapter에서 끝에 '|' 문자를 제거하므로 |를 붙여줌 안붙인다면 괄호가 삭제되는 있으므로 | 붙여줌
                db.chatRoomDao().insertChatRoom(chatRoom)
            } else { //기존에 방이 존재한다면
                val chatRoom = db.chatRoomDao().getRoomFromName(messageData.roomName)
                //chatroom에 메시지 추가
                chatRoom.messageDatas =
                    chatRoom.messageDatas + message + "|" //"," 기준으로 message를 구분하기 위해 끝에 | 를 붙여줌

                chatRoom.let {
                    chatRoom.unReadCount += 1
                    db.chatRoomDao().updateChatRoom(it)
                }
            }
        }

        fun joinUserInRoom(message: String) {

        }
    }
}
