package rageofachilles.textryan_wear;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
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
import android.provider.ContactsContract.CommonDataKinds.Phone;
import java.util.ArrayList;

/**
 * Created by Ryan on 5/15/2016.
 */
public class ContactDialogPreference extends DialogPreference
{
    private Uri QUERY_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    private String CONTACT_ID = ContactsContract.Contacts._ID;
    private String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
    private String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;
    private String PHONE_NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;
    private int MAX_STRING = 20;

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
        ContentResolver cr = getContext().getContentResolver();

        String[] projection = new String[]{CONTACT_ID, DISPLAY_NAME, HAS_PHONE_NUMBER, Phone.TYPE, PHONE_NUMBER};

        Cursor cursor = cr.query(QUERY_URI, projection, ContactsContract.Contacts.HAS_PHONE_NUMBER, null,  ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");

        ArrayList<Contact> contacts = new ArrayList<>();

        String oldName = "";
        int count =0;
        while (cursor.moveToNext()) {

            String name = (cursor.getString(cursor.getColumnIndex(DISPLAY_NAME))).trim();
            final int typeIndex = cursor.getColumnIndex(Phone.TYPE);
            final int numberIndex = cursor.getColumnIndex(PHONE_NUMBER);

            int type = cursor.getInt(typeIndex);
            String phoneNumber = cursor.getString(numberIndex);

            if(name.equals(oldName)) {
                oldName = name;
                name = ""; // Don't display multiple names for same contact
            } else {
                oldName = name;
            }
            contacts.add(new Contact(name, phoneNumber, TypeToString(type) ));
            count++;
        }
        Log.v("myTag",""+count);
        cursor.close();
        return contacts;
    }

    protected String TypeToString(int type)
    {
        switch (type) {
            case Phone.TYPE_HOME:
                return "Home";
            case Phone.TYPE_MOBILE:
                return "Mobile";
            case Phone.TYPE_WORK:
                return "Work";
            case Phone.TYPE_FAX_HOME:
                return "Home Fax";
            case Phone.TYPE_FAX_WORK:
                return "Work Fax";
            case Phone.TYPE_MAIN:
                return "Main";
            case Phone.TYPE_OTHER:
                return "Other";
            case Phone.TYPE_CUSTOM:
                return "Custom";
            case Phone.TYPE_PAGER:
                return "Pager";
            default:
                return "";
        }
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

            if(row == null) {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ViewHolder();
                holder.textView1 = (TextView)row.findViewById(R.id.lblName);
                holder.textView2 = (TextView)row.findViewById(R.id.lblType);
                holder.textView3 = (TextView)row.findViewById(R.id.lblNumber);
                row.setTag(holder);
            } else {
                holder = (ViewHolder)row.getTag();
            }

            Contact person = data.get(position);

            String name = person.getName();
            if (name.length() > MAX_STRING) {
                name = name.substring(0, MAX_STRING);
                name = name.concat("...");
            }
            holder.textView1.setText(name);
            holder.textView2.setText(person.getType());
            holder.textView3.setText(person.getNumber());
            return row;
        }

        class ViewHolder
        {
            TextView textView1;
            TextView textView2;
            TextView textView3;
        }
    }

    public class Contact
    {
        String name;
        String number;
        String type;
        // Picture
        public Contact (String input_name, String input_number, String input_type) {
            name = input_name;
            number = input_number;
            type = input_type;
        }

        public String getName(){
            return name;
        }

        public String getNumber(){
            return number;
        }

        public String getType(){
            return type;
        }
    }
}

