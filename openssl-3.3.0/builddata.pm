package OpenSSL::safe::installdata;

use strict;
use warnings;
use Exporter;
our @ISA = qw(Exporter);
our @EXPORT = qw($PREFIX
                  $BINDIR $BINDIR_REL
                  $LIBDIR $LIBDIR_REL
                  $INCLUDEDIR $INCLUDEDIR_REL
                  $APPLINKDIR $APPLINKDIR_REL
                  $ENGINESDIR $ENGINESDIR_REL
                  $MODULESDIR $MODULESDIR_REL
                  $PKGCONFIGDIR $PKGCONFIGDIR_REL
                  $CMAKECONFIGDIR $CMAKECONFIGDIR_REL
                  $VERSION @LDLIBS);

our $PREFIX             = '/home/lumkit/my_work/all/openssl-3.3.0';
our $BINDIR             = '/home/lumkit/my_work/all/openssl-3.3.0/apps';
our $BINDIR_REL         = 'apps';
our $LIBDIR             = '/home/lumkit/my_work/all/openssl-3.3.0';
our $LIBDIR_REL         = '.';
our $INCLUDEDIR         = '/home/lumkit/my_work/all/openssl-3.3.0/include';
our $INCLUDEDIR_REL     = 'include';
our $APPLINKDIR         = '/home/lumkit/my_work/all/openssl-3.3.0/ms';
our $APPLINKDIR_REL     = 'ms';
our $ENGINESDIR         = '/home/lumkit/my_work/all/openssl-3.3.0/engines';
our $ENGINESDIR_REL     = 'engines';
our $MODULESDIR         = '/home/lumkit/my_work/all/openssl-3.3.0/providers';
our $MODULESDIR_REL     = 'providers';
our $PKGCONFIGDIR       = '';
our $PKGCONFIGDIR_REL   = '';
our $CMAKECONFIGDIR     = '';
our $CMAKECONFIGDIR_REL = '';
our $VERSION            = '3.3.0';
our @LDLIBS             =
    # Unix and Windows use space separation, VMS uses comma separation
    split(/ +| *, */, '-ldl -pthread ');

1;
