package rageofachilles.textryan_wear;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.DialogPreference;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Ryan on 5/15/2016.
 */
public class ContactDialogPreference extends DialogPreference
{



    private Uri QUERY_URI = ContactsContract.Contacts.CONTENT_URI;
    private String CONTACT_ID = ContactsContract.Contacts._ID;
    private String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
    private Uri EMAIL_CONTENT_URI = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
    private String EMAIL_CONTACT_ID = ContactsContract.CommonDataKinds.Email.CONTACT_ID;
    private String EMAIL_DATA = ContactsContract.CommonDataKinds.Email.DATA;
    private String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
    private String PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
    private Uri PHONE_CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    private String PHONE_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
    private String STARRED_CONTACT = ContactsContract.Contacts.STARRED;




    public ContactDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ContactDialogPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            // save shared preferences
        }
    }

    private void init(Context context) {
        setPersistent(false);
        setDialogLayoutResource(R.layout.contacts_layout);

    }


    public ArrayList<Contact> getContacts()
    {
        ArrayList<Contact> contactList = new ArrayList<>();

        //String selection = ContactsContract.Contacts.DISPLAY_NAME ;//+ " LIKE ? and " + ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0 ";
        ContentResolver cr = getContext().getContentResolver();

        String[] projection = new String[]{CONTACT_ID, DISPLAY_NAME, HAS_PHONE_NUMBER, STARRED_CONTACT};
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                projection, null, null, null);

//        if (cur.getCount() > 0) {
//                    while (cur.moveToNext()) {
//                        if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0){
//                    String name = cur.getString(cur.getColumnIndex(
//                            ContactsContract.Contacts.DISPLAY_NAME));
//
//                    String contactId = cur.getString(cur.getColumnIndex(CONTACT_ID));
//                    Cursor phoneCursor = cr2.query(ContactsContract.Contacts.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{contactId}, null);
//                    while (phoneCursor.moveToNext()) {
//                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(PHONE_NUMBER));
//                        String number = phoneNumber;
//                        contactList.add(new Contact(name, number));
//                    }
//
//                    phoneCursor.close();
//                }
//            }
//            cur.close();
//        }

        Cursor cursor = cr.query(QUERY_URI, projection, null, null,  ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");

        while (cursor.moveToNext()) {
            ArrayList<Contact> contacts = getContact(cursor);
            for (Contact cont : contacts) {
                contactList.add(cont);
            }
        }

        cursor.close();
        return contactList;
    }

    private ArrayList<Contact> getContact(Cursor cursor) {
        ArrayList<Contact> contacts = new ArrayList<>();
        String contactId = cursor.getString(cursor.getColumnIndex(CONTACT_ID));
        String name = (cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
        Uri uri = Uri.withAppendedPath(QUERY_URI, String.valueOf(contactId));

        ArrayList<String> numbers = getPhone(cursor, contactId);
        for(String num : numbers) {
            contacts.add(new Contact(name, num, contactId));
        }

        return contacts;
    }

    private ArrayList<String> getPhone(Cursor cursor, String contactId) {
        ArrayList<String> numbers = new ArrayList<>();
        ContentResolver contentResolver = getContext().getContentResolver();
        int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
        Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));
        if (hasPhoneNumber > 0) {
            Cursor phoneCursor = contentResolver.query(PHONE_CONTENT_URI, null, PHONE_CONTACT_ID + " = ?", new String[]{contactId}, null);
            while (phoneCursor.moveToNext()) {
                int type = phoneCursor.getInt(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE || type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(PHONE_NUMBER));
                    numbers.add(phoneNumber);
                }
            }
            phoneCursor.close();
        }
        return numbers;
    }

    @Override
    protected void onBindDialogView(View view)
    {
        ListView lv = ((ListView) view.findViewById(R.id.lvContact));
        ArrayList<Contact> contactList = getContacts();

        // Get Contacts
        getContacts();

        // Set the ArrayAdapter as the ListView's adapter.
        lv.setAdapter( new ContactAdapter(getContext(), R.layout.contact_layout, contactList) );

        super.onBindDialogView(view);
    }




    public class ContactAdapter extends ArrayAdapter<Contact>
    {
        private final Context context;
        private final ArrayList<Contact> data;
        private final int layoutResourceId;

        public ContactAdapter(Context context, int layoutResourceId, ArrayList<Contact> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
            this.layoutResourceId = layoutResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if(row == null)
            {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ViewHolder();
                holder.textView1 = (TextView)row.findViewById(R.id.lblName);
                holder.textView2 = (TextView)row.findViewById(R.id.lblNumber);

                row.setTag(holder);
            }
            else
            {
                holder = (ViewHolder)row.getTag();
            }

            Contact person = data.get(position);

            holder.textView1.setText(person.getName());
            holder.textView2.setText(person.getNumber());

            return row;
        }

        class ViewHolder
        {
            TextView textView1;
            TextView textView2;

        }
    }


    public class Contact
    {
        String name;
        String number;
        String id;
        // Picture
        public Contact (String input_name, String input_number, String input_id)
        {
            name = input_name;
            number = input_number;
            id = input_id;
        }

        public String getName(){
            return name;
        }

        public String getNumber(){
            return number;
        }
    }
}

