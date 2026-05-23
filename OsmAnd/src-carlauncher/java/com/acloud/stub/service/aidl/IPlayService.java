package com.acloud.stub.service.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * XYAuto Yerel Muzik calar servisi AIDL arayuzunun Java karsiligi.
 * Kod icerisinde kesinlikle Turkce karakter kullanilmamistir.
 */
public interface IPlayService extends IInterface {
    
    void init() throws RemoteException;
    void start() throws RemoteException;
    void pause() throws RemoteException;
    void stop() throws RemoteException;
    int getDuration() throws RemoteException;
    int getPosition() throws RemoteException;
    int getState() throws RemoteException;
    void seekTo(int position) throws RemoteException;

    public static abstract class Stub extends Binder implements IPlayService {
        private static final String DESCRIPTOR = "com.acloud.stub.service.aidl.IPlayService";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPlayService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IPlayService) {
                return (IPlayService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_init: {
                    data.enforceInterface(DESCRIPTOR);
                    this.init();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_start: {
                    data.enforceInterface(DESCRIPTOR);
                    this.start();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_pause: {
                    data.enforceInterface(DESCRIPTOR);
                    this.pause();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stop: {
                    data.enforceInterface(DESCRIPTOR);
                    this.stop();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getDuration: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getDuration();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_getPosition: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getPosition();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_getState: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getState();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_seekTo: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0 = data.readInt();
                    this.seekTo(_arg0);
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IPlayService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void init() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_init, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void start() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void pause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getDuration() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getDuration, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int getPosition() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getPosition, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int getState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getState, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void seekTo(int position) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(position);
                    mRemote.transact(Stub.TRANSACTION_seekTo, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_init = (IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_start = (IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_pause = (IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_stop = (IBinder.FIRST_CALL_TRANSACTION + 3);
        static final int TRANSACTION_getDuration = (IBinder.FIRST_CALL_TRANSACTION + 4);
        static final int TRANSACTION_getPosition = (IBinder.FIRST_CALL_TRANSACTION + 5);
        static final int TRANSACTION_getState = (IBinder.FIRST_CALL_TRANSACTION + 6);
        static final int TRANSACTION_seekTo = (IBinder.FIRST_CALL_TRANSACTION + 7);
    }
}
