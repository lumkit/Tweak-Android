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

our $PREFIX             = '/home/lumkit/my_work/all/openssl/x86_64';
our $BINDIR             = '/home/lumkit/my_work/all/openssl/x86_64/bin';
our $BINDIR_REL         = 'bin';
our $LIBDIR             = '/home/lumkit/my_work/all/openssl/x86_64/lib';
our $LIBDIR_REL         = 'lib';
our $INCLUDEDIR         = '/home/lumkit/my_work/all/openssl/x86_64/include';
our $INCLUDEDIR_REL     = 'include';
our $APPLINKDIR         = '/home/lumkit/my_work/all/openssl/x86_64/include/openssl';
our $APPLINKDIR_REL     = 'include/openssl';
our $ENGINESDIR         = '/home/lumkit/my_work/all/openssl/x86_64/lib/engines-3';
our $ENGINESDIR_REL     = 'lib/engines-3';
our $MODULESDIR         = '/home/lumkit/my_work/all/openssl/x86_64/lib/ossl-modules';
our $MODULESDIR_REL     = 'lib/ossl-modules';
our $PKGCONFIGDIR       = '/home/lumkit/my_work/all/openssl/x86_64/lib/pkgconfig';
our $PKGCONFIGDIR_REL   = 'lib/pkgconfig';
our $CMAKECONFIGDIR     = '/home/lumkit/my_work/all/openssl/x86_64/lib/cmake/OpenSSL';
our $CMAKECONFIGDIR_REL = 'lib/cmake/OpenSSL';
our $VERSION            = '3.3.0';
our @LDLIBS             =
    # Unix and Windows use space separation, VMS uses comma separation
    split(/ +| *, */, '-ldl -pthread ');

1;
